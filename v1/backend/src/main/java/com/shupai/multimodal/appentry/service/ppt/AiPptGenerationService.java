package com.shupai.multimodal.appentry.service.ppt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shupai.multimodal.appentry.model.entity.ModelServiceEntity;
import com.shupai.multimodal.appentry.model.entity.TaskEntity;
import com.shupai.multimodal.appentry.model.ppt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiPptGenerationService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final List<String> LAYOUTS = List.of(
            "cover-hero", "agenda-list", "section-divider", "key-message", "three-cards",
            "split-compare", "timeline-roadmap", "metric-cards", "summary-actions"
    );

    private final ObjectMapper objectMapper;

    public AiPptGenerationResult generateDeckPlan(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        String rawResponse = "{}";
        AiPptDeckPlan deckPlan;
        try {
            String content = invokeChat(service, buildDeckPrompt(task, options), options);
            rawResponse = content;
            deckPlan = normalizeDeckPlan(readDeckPlan(content), task, service, options);
        } catch (Exception ex) {
            log.warn("AI PPT deck planning failed, fallback to deterministic deck, taskId={}", task.getTaskId(), ex);
            deckPlan = fallbackDeckPlan(task.getInputText(), service, options);
        }
        return new AiPptGenerationResult(deckPlan, rawResponse);
    }

    public AiPptDeckPlan rewriteSlide(AiPptDeckPlan deckPlan, String slideId, String instruction, ModelServiceEntity service) {
        if (service == null || !StringUtils.hasText(slideId)) {
            return deterministicRewrite(deckPlan, slideId, instruction);
        }
        try {
            String currentSlide = objectMapper.writeValueAsString(findSlide(deckPlan, slideId));
            String prompt = """
                    你是资深 PPT 页面设计专家。请根据用户指令只重写当前 slide，不要修改其他页面。
                    必须保持 JSON Schema 不变，只输出单个 slide JSON，不要 Markdown。
                    用户指令：%s
                    当前 slide：%s
                    """.formatted(instruction, currentSlide);
            String content = invokeChat(service, prompt, Map.of("temperature", 0.4D));
            AiPptSlidePlan rewritten = objectMapper.convertValue(parseJson(content), AiPptSlidePlan.class);
            return replaceSlide(deckPlan, normalizeSlide(rewritten, indexOfSlide(deckPlan, slideId) + 1));
        } catch (Exception ex) {
            log.warn("AI PPT slide rewrite failed, slideId={}", slideId, ex);
            return deterministicRewrite(deckPlan, slideId, instruction);
        }
    }

    public AiPptDeckPlan optimizeDeck(AiPptDeckPlan deckPlan, String instruction, ModelServiceEntity service) {
        if (service == null) {
            return deterministicOptimize(deckPlan, instruction);
        }
        try {
            String prompt = """
                    你是资深咨询顾问和演示文稿设计总监。请根据用户指令优化整份 PPT DeckPlan。
                    保留用户已经明确表达的业务含义，不要删除关键页面，保持 JSON Schema 不变，只输出 JSON。
                    用户指令：%s
                    当前 DeckPlan：%s
                    """.formatted(instruction, objectMapper.writeValueAsString(deckPlan));
            String content = invokeChat(service, prompt, Map.of("temperature", 0.45D));
            return normalizeDeckPlan(readDeckPlan(content), null, service, Map.of());
        } catch (Exception ex) {
            log.warn("AI PPT deck optimize failed", ex);
            return deterministicOptimize(deckPlan, instruction);
        }
    }

    private String buildDeckPrompt(TaskEntity task, Map<String, Object> options) {
        int pages = intOption(options, "pages", 10);
        return """
                你是一个由资深咨询顾问、PPT 策划专家和视觉设计师组成的 AI PPT 生成系统。
                你的任务不是简单生成大纲，而是生成可以被程序渲染为 PPTX 的结构化 PPT 设计稿。
                严格输出 JSON，不要 Markdown。页面数量尽量接近 %d 页。
                每页只表达一个核心观点。每页必须包含 title、takeaway、content、layout、visual、speakerNotes。
                可用 layout：cover-hero, agenda-list, section-divider, key-message, three-cards, split-compare, timeline-roadmap, metric-cards, summary-actions。
                输出 Schema：{"deck":{"title":"","audience":"","scenario":"","goal":"","language":"zh-CN","tone":"","pageCount":0,"designDirection":""},"theme":{"primaryColor":"#12355B","secondaryColor":"#5D7491","accentColor":"#DB7640","backgroundColor":"#FAFBFC","textColor":"#1F2937","fontFamily":"Microsoft YaHei","visualStyle":"consulting-clean"},"storyline":[],"slides":[{"id":"slide_01","page":1,"role":"cover","type":"cover","title":"","subtitle":"","takeaway":"","content":[{"type":"point","title":"","text":""}],"layout":{"type":"cover-hero","composition":"","density":"medium","emphasis":""},"visual":{"icons":[],"imagePrompt":"","chartSpec":null,"background":{"type":"solid","color":"#FAFBFC"}},"speakerNotes":""}]}。
                用户输入：%s
                当前选项：%s
                """.formatted(pages, defaultString(task.getInputText(), "生成一份汇报 PPT"), toJson(options));
    }

    private String invokeChat(ModelServiceEntity service, String prompt, Map<String, Object> options) throws Exception {
        if (service == null || !StringUtils.hasText(service.getEndpoint())) {
            throw new IllegalStateException("model service endpoint is empty");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", defaultString(service.getModelCode(), service.getModelName()));
        body.put("temperature", doubleOption(options, "temperature", 0.5D));
        body.put("messages", List.of(
                Map.of("role", "system", "content", "你是专业的 PPT 生成系统，只返回 JSON。"),
                Map.of("role", "user", "content", prompt)
        ));
        HttpRequest.Builder builder = HttpRequest.newBuilder(resolveChatEndpoint(service))
                .timeout(Duration.ofSeconds(resolveTimeoutSeconds(service)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(body)));
        if (!StringUtils.hasText(service.getAuthType()) || "bearer".equalsIgnoreCase(service.getAuthType())) {
            builder.header("Authorization", "Bearer " + resolveBearerToken(service));
        }
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("AI service failed: " + response.statusCode() + " " + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        String text = extractText(root);
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException("AI service returned empty content");
        }
        return text;
    }

    private URI resolveChatEndpoint(ModelServiceEntity service) {
        String endpoint = service.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalStateException("model service endpoint is empty");
        }
        String normalized = endpoint.trim();
        if (normalized.endsWith("/chat/completions")) {
            return URI.create(normalized);
        }
        return URI.create(normalized.replaceAll("/+$", "") + "/chat/completions");
    }

    private AiPptDeckPlan readDeckPlan(String content) throws JsonProcessingException {
        return objectMapper.convertValue(parseJson(content), AiPptDeckPlan.class);
    }

    private Object parseJson(String content) throws JsonProcessingException {
        String json = stripMarkdownFence(content);
        return objectMapper.readValue(json, MAP_TYPE);
    }

    private AiPptDeckPlan normalizeDeckPlan(AiPptDeckPlan input, TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        String fallbackTitle = task == null ? "AI PPT" : defaultString(task.getInputText(), "AI PPT");
        DeckMeta sourceDeck = input == null ? null : input.deck();
        AiPptTheme sourceTheme = input == null ? null : input.theme();
        List<AiPptSlidePlan> sourceSlides = input == null || input.slides() == null ? List.of() : input.slides();
        DeckMeta deck = new DeckMeta(
                defaultString(sourceDeck == null ? null : sourceDeck.title(), fallbackTitle),
                defaultString(sourceDeck == null ? null : sourceDeck.audience(), stringOption(options, "audience", "目标受众")),
                defaultString(sourceDeck == null ? null : sourceDeck.scenario(), stringOption(options, "scenario", "汇报")),
                defaultString(sourceDeck == null ? null : sourceDeck.goal(), "说明背景、方案、路径和价值"),
                defaultString(sourceDeck == null ? null : sourceDeck.language(), "zh-CN"),
                defaultString(sourceDeck == null ? null : sourceDeck.tone(), stringOption(options, "style", "consulting")),
                sourceDeck != null && sourceDeck.pageCount() != null ? sourceDeck.pageCount() : intOption(options, "pages", Math.max(6, sourceSlides.size())),
                defaultString(sourceDeck == null ? null : sourceDeck.designDirection(), "清晰、专业、适合管理层汇报"),
                service == null ? null : service.getServiceCode(),
                service == null ? null : defaultString(service.getModelName(), service.getModelCode())
        );
        AiPptTheme theme = normalizeTheme(sourceTheme, stringOption(options, "template", "consulting-clean"));
        List<AiPptSlidePlan> slides = new ArrayList<>();
        if (sourceSlides.isEmpty()) {
            return fallbackDeckPlan(deck.title(), service, options);
        }
        for (int i = 0; i < sourceSlides.size(); i++) {
            slides.add(normalizeSlide(sourceSlides.get(i), i + 1));
        }
        return new AiPptDeckPlan(deck, theme, input.storyline() == null ? inferStoryline(slides) : input.storyline(), slides);
    }

    private AiPptSlidePlan normalizeSlide(AiPptSlidePlan slide, int page) {
        String id = defaultString(slide == null ? null : slide.id(), "slide_%02d".formatted(page));
        String layoutType = normalizeLayout(slide != null && slide.layout() != null ? slide.layout().type() : null, page);
        List<AiPptContentBlock> content = slide == null || slide.content() == null ? List.of() : slide.content();
        if (content.isEmpty()) {
            content = List.of(new AiPptContentBlock("point", "核心要点", defaultString(slide == null ? null : slide.takeaway(), "补充关键观点和行动建议。"), null, null, null));
        }
        return new AiPptSlidePlan(
                id,
                page,
                defaultString(slide == null ? null : slide.role(), page == 1 ? "cover" : "content"),
                defaultString(slide == null ? null : slide.type(), "content"),
                defaultString(slide == null ? null : slide.title(), page == 1 ? "汇报标题" : "页面标题"),
                slide == null ? null : slide.subtitle(),
                defaultString(slide == null ? null : slide.takeaway(), firstBlockText(content)),
                content,
                new AiPptLayoutSpec(layoutType, slide != null && slide.layout() != null ? slide.layout().composition() : null, "medium", slide != null && slide.layout() != null ? slide.layout().emphasis() : null),
                slide != null && slide.visual() != null ? slide.visual() : new AiPptVisualSpec(List.of(), null, null, null, new AiPptBackground("solid", "#FAFBFC")),
                defaultString(slide == null ? null : slide.speakerNotes(), "围绕本页核心观点展开说明。")
        );
    }

    public AiPptDeckPlan fallbackDeckPlan(String prompt, ModelServiceEntity service, Map<String, Object> options) {
        String title = defaultString(prompt, "AI 智能汇报方案");
        int pages = Math.max(6, intOption(options, "pages", 8));
        AiPptTheme theme = normalizeTheme(null, stringOption(options, "template", "consulting-clean"));
        List<AiPptSlidePlan> slides = new ArrayList<>();
        slides.add(slide(1, "cover", "cover", title, "围绕背景、方案、路径和价值形成完整汇报", "cover-hero", List.of("汇报目标", "核心议题")));
        slides.add(slide(2, "agenda", "agenda", "目录", "本次汇报从背景、方案、路径和价值展开", "agenda-list", List.of("背景与目标", "核心方案", "实施路径", "收益与下一步")));
        slides.add(slide(3, "background", "content", "背景与挑战", "先明确为什么现在需要推进该主题", "three-cards", List.of("业务环境变化带来新的效率要求", "现有流程存在协同和响应瓶颈", "需要通过系统化方案形成可落地路径")));
        slides.add(slide(4, "solution", "content", "总体方案", "以结构化能力建设支撑目标落地", "key-message", List.of("明确目标场景和核心用户", "沉淀标准流程和关键能力", "通过阶段化实施降低风险")));
        slides.add(slide(5, "roadmap", "timeline", "实施路径", "分阶段推进更利于控制风险和验证价值", "timeline-roadmap", List.of("需求确认", "方案设计", "试点验证", "推广运营")));
        slides.add(slide(6, "metrics", "metrics", "预期收益", "用指标衡量方案是否产生真实业务价值", "metric-cards", List.of("效率提升", "成本优化", "体验改善")));
        slides.add(slide(7, "summary", "summary", "总结与下一步", "形成明确行动项推动后续执行", "summary-actions", List.of("确认范围", "明确负责人", "启动试点")));
        return new AiPptDeckPlan(
                new DeckMeta(title, stringOption(options, "audience", "管理层"), stringOption(options, "scenario", "汇报"), "形成可执行的结构化汇报", "zh-CN", stringOption(options, "style", "consulting"), pages, "专业、清晰、适合正式汇报", service == null ? null : service.getServiceCode(), service == null ? null : defaultString(service.getModelName(), service.getModelCode())),
                theme,
                List.of("背景与挑战", "总体方案", "实施路径", "预期收益", "下一步行动"),
                slides
        );
    }

    private AiPptSlidePlan slide(int page, String role, String type, String title, String takeaway, String layout, List<String> points) {
        List<AiPptContentBlock> blocks = points.stream().map(point -> new AiPptContentBlock("point", point, point, null, null, null)).toList();
        return new AiPptSlidePlan("slide_%02d".formatted(page), page, role, type, title, null, takeaway, blocks, new AiPptLayoutSpec(layout, null, "medium", null), new AiPptVisualSpec(List.of(), null, null, null, new AiPptBackground("solid", "#FAFBFC")), "围绕本页观点补充业务背景、证据和行动建议。");
    }

    private AiPptDeckPlan deterministicRewrite(AiPptDeckPlan deckPlan, String slideId, String instruction) {
        AiPptSlidePlan slide = findSlide(deckPlan, slideId);
        if (slide == null) {
            return deckPlan;
        }
        String layout = instruction != null && instruction.contains("对比") ? "split-compare" : instruction != null && instruction.contains("指标") ? "metric-cards" : slide.layout().type();
        AiPptSlidePlan rewritten = new AiPptSlidePlan(slide.id(), slide.page(), slide.role(), slide.type(), slide.title(), slide.subtitle(), defaultString(instruction, slide.takeaway()), slide.content(), new AiPptLayoutSpec(layout, slide.layout().composition(), slide.layout().density(), slide.layout().emphasis()), slide.visual(), slide.speakerNotes());
        return replaceSlide(deckPlan, rewritten);
    }

    private AiPptDeckPlan deterministicOptimize(AiPptDeckPlan deckPlan, String instruction) {
        if (deckPlan == null) {
            return null;
        }
        AiPptTheme theme = instruction != null && instruction.contains("科技") ? normalizeTheme(null, "tech-dark") : deckPlan.theme();
        return new AiPptDeckPlan(deckPlan.deck(), theme, deckPlan.storyline(), deckPlan.slides());
    }

    private AiPptDeckPlan replaceSlide(AiPptDeckPlan deckPlan, AiPptSlidePlan rewritten) {
        List<AiPptSlidePlan> slides = new ArrayList<>();
        for (AiPptSlidePlan slide : deckPlan.slides()) {
            slides.add(slide.id().equals(rewritten.id()) ? rewritten : slide);
        }
        return new AiPptDeckPlan(deckPlan.deck(), deckPlan.theme(), deckPlan.storyline(), slides);
    }

    private AiPptSlidePlan findSlide(AiPptDeckPlan deckPlan, String slideId) {
        if (deckPlan == null || deckPlan.slides() == null) {
            return null;
        }
        return deckPlan.slides().stream().filter(slide -> slideId.equals(slide.id())).findFirst().orElse(null);
    }

    private int indexOfSlide(AiPptDeckPlan deckPlan, String slideId) {
        if (deckPlan == null || deckPlan.slides() == null) {
            return 0;
        }
        for (int i = 0; i < deckPlan.slides().size(); i++) {
            if (slideId.equals(deckPlan.slides().get(i).id())) {
                return i;
            }
        }
        return 0;
    }

    private AiPptTheme normalizeTheme(AiPptTheme theme, String template) {
        if (theme != null && StringUtils.hasText(theme.primaryColor())) {
            return theme;
        }
        return switch (defaultString(template, "consulting-clean")) {
            case "tech-dark" -> new AiPptTheme("#081020", "#1C385E", "#40DEFF", "#EDF4FC", "#121C2C", "Microsoft YaHei", "tech-dark");
            case "business-blue", "business" -> new AiPptTheme("#12355B", "#5D7491", "#E07A5F", "#F8FAFC", "#1F2937", "Microsoft YaHei", "business-blue");
            case "finance-green" -> new AiPptTheme("#0E4334", "#226E54", "#CDA649", "#F6FAF7", "#1C302A", "Microsoft YaHei", "finance-green");
            case "government-red" -> new AiPptTheme("#751C24", "#96373F", "#DBB25C", "#FCF8F2", "#412D2D", "Microsoft YaHei", "government-red");
            default -> new AiPptTheme("#122237", "#5D7491", "#DB7640", "#FAFBFC", "#1F2937", "Microsoft YaHei", "consulting-clean");
        };
    }

    private String normalizeLayout(String layout, int page) {
        if (StringUtils.hasText(layout) && LAYOUTS.contains(layout)) {
            return layout;
        }
        return page == 1 ? "cover-hero" : page == 2 ? "agenda-list" : "key-message";
    }

    private List<String> inferStoryline(List<AiPptSlidePlan> slides) {
        return slides.stream().map(AiPptSlidePlan::title).limit(6).toList();
    }

    private String extractText(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isTextual()) {
                return content.asText();
            }
        }
        if (root.hasNonNull("output_text")) {
            return root.get("output_text").asText();
        }
        if (root.hasNonNull("text")) {
            return root.get("text").asText();
        }
        return null;
    }

    private String stripMarkdownFence(String value) {
        if (!StringUtils.hasText(value)) {
            return "{}";
        }
        String trimmed = value.trim().replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        return start >= 0 && end > start ? trimmed.substring(start, end + 1) : trimmed;
    }

    private String resolveBearerToken(ModelServiceEntity service) {
        if (StringUtils.hasText(service.getSecretRef())) {
            String value = System.getenv(service.getSecretRef().trim());
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        if (StringUtils.hasText(service.getApiKeySecret())) {
            return service.getApiKeySecret();
        }
        String normalized = service.getServiceCode() == null ? "" : service.getServiceCode().replaceAll("[^A-Za-z0-9]", "_").toUpperCase();
        for (String key : List.of("MODEL_SERVICE_API_KEY__" + normalized, "MODEL_SERVICE_API_KEY", "OPENROUTER_API_KEY", "OPENAI_API_KEY")) {
            String value = System.getenv(key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        throw new IllegalStateException("Bearer token not found");
    }

    private int resolveTimeoutSeconds(ModelServiceEntity service) {
        return Math.max(10, Math.min(180, service.getTimeoutMs() == null ? 60 : service.getTimeoutMs() / 1000));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private int intOption(Map<String, Object> options, String key, int fallback) {
        Object value = options == null ? null : options.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private double doubleOption(Map<String, Object> options, String key, double fallback) {
        Object value = options == null ? null : options.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String stringOption(Map<String, Object> options, String key, String fallback) {
        Object value = options == null ? null : options.get(key);
        return value == null || !StringUtils.hasText(String.valueOf(value)) ? fallback : String.valueOf(value);
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String firstBlockText(List<AiPptContentBlock> content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        AiPptContentBlock block = content.get(0);
        return defaultString(block.text(), defaultString(block.title(), ""));
    }
}
