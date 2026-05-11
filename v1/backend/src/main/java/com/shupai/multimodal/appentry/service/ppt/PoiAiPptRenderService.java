package com.shupai.multimodal.appentry.service.ppt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shupai.multimodal.appentry.model.ppt.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PoiAiPptRenderService implements PptRenderService {

    private final ObjectMapper objectMapper;

    @Override
    public GeneratedPptx render(AiPptDeckPlan deckPlan) {
        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ppt.setPageSize(new Dimension(1280, 720));
            AiPptDeckPlan normalized = deckPlan == null ? emptyDeck() : deckPlan;
            for (AiPptSlidePlan slide : normalized.slides()) {
                renderSlide(ppt, normalized, slide);
            }
            ppt.write(output);
            return new GeneratedPptx(output.toByteArray(), "application/vnd.openxmlformats-officedocument.presentationml.presentation", objectMapper.writeValueAsString(deckPlan));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render AI PPTX", ex);
        }
    }

    private void renderSlide(XMLSlideShow ppt, AiPptDeckPlan deckPlan, AiPptSlidePlan slide) {
        String layout = slide.layout() == null ? "key-message" : slide.layout().type();
        switch (layout) {
            case "cover-hero" -> renderCover(ppt, deckPlan, slide);
            case "agenda-list" -> renderAgenda(ppt, deckPlan, slide);
            case "section-divider" -> renderSection(ppt, deckPlan, slide);
            case "three-cards" -> renderCards(ppt, deckPlan, slide, 3);
            case "split-compare" -> renderCompare(ppt, deckPlan, slide);
            case "timeline-roadmap" -> renderTimeline(ppt, deckPlan, slide);
            case "metric-cards" -> renderCards(ppt, deckPlan, slide, 3);
            case "summary-actions" -> renderSection(ppt, deckPlan, slide);
            default -> renderKeyMessage(ppt, deckPlan, slide);
        }
    }

    private void renderCover(XMLSlideShow ppt, AiPptDeckPlan deckPlan, AiPptSlidePlan slide) {
        XSLFSlide s = ppt.createSlide();
        Theme t = theme(deckPlan);
        addBackground(s, t.primary());
        addAccent(s, 880, -80, 420, 420, t.accent(), 0.2);
        addAccent(s, -130, 520, 360, 260, t.secondary(), 0.25);
        addText(s, slide.title(), 88, 150, 890, 150, 46D, true, Color.WHITE);
        addText(s, text(slide.subtitle(), slide.takeaway()), 92, 318, 900, 55, 22D, false, new Color(220, 230, 242));
        addText(s, text(deckPlan.deck() == null ? null : deckPlan.deck().scenario(), "AI 智能生成"), 92, 420, 700, 34, 18D, false, t.accent());
    }

    private void renderAgenda(XMLSlideShow ppt, AiPptDeckPlan deckPlan, AiPptSlidePlan slide) {
        XSLFSlide s = ppt.createSlide();
        Theme t = theme(deckPlan);
        addBackground(s, t.surface());
        addTopRule(s, t.accent());
        addText(s, text(slide.title(), "目录"), 72, 58, 760, 64, 34D, true, t.primary());
        List<String> items = deckPlan.storyline() == null || deckPlan.storyline().isEmpty()
                ? deckPlan.slides().stream().skip(1).map(AiPptSlidePlan::title).limit(8).toList()
                : deckPlan.storyline();
        int y = 150;
        for (int i = 0; i < Math.min(8, items.size()); i++) {
            addText(s, "%02d".formatted(i + 1), 92, y, 70, 34, 22D, true, t.accent());
            addText(s, items.get(i), 170, y, 720, 34, 22D, false, t.primary());
            y += 52;
        }
        addAccent(s, 930, 150, 230, 360, t.secondary(), 0.25);
    }

    private void renderSection(XMLSlideShow ppt, AiPptDeckPlan deckPlan, AiPptSlidePlan slide) {
        XSLFSlide s = ppt.createSlide();
        Theme t = theme(deckPlan);
        addBackground(s, t.primary());
        addAccent(s, 820, 80, 360, 360, t.accent(), 0.2);
        addText(s, "%02d".formatted(slide.page() == null ? ppt.getSlides().size() : slide.page()), 86, 115, 160, 54, 28D, true, t.accent());
        addText(s, slide.title(), 86, 198, 900, 100, 42D, true, Color.WHITE);
        addText(s, text(slide.takeaway(), slide.subtitle()), 90, 320, 880, 82, 22D, false, new Color(220, 230, 242));
    }

    private void renderKeyMessage(XMLSlideShow ppt, AiPptDeckPlan deckPlan, AiPptSlidePlan slide) {
        XSLFSlide s = ppt.createSlide();
        Theme t = theme(deckPlan);
        addBackground(s, t.surface());
        addTopRule(s, t.accent());
        addText(s, slide.title(), 68, 48, 850, 62, 30D, true, t.primary());
        addPanel(s, 84, 150, 780, 80, t.panel());
        addText(s, "核心观点", 112, 165, 120, 24, 14D, true, t.secondary());
        addText(s, text(slide.takeaway(), firstContent(slide)), 112, 194, 700, 30, 18D, true, t.primary());
        addBullets(s, slide.content(), 100, 268, 760, 300, 19D, t.text());
        addPanel(s, 925, 158, 260, 360, t.panel());
        addText(s, "演讲提示", 950, 185, 220, 34, 18D, true, t.primary());
        addText(s, text(slide.speakerNotes(), firstContent(slide)), 950, 238, 210, 160, 16D, false, t.text());
    }

    private void renderCards(XMLSlideShow ppt, AiPptDeckPlan deckPlan, AiPptSlidePlan slide, int limit) {
        XSLFSlide s = ppt.createSlide();
        Theme t = theme(deckPlan);
        addBackground(s, t.surface());
        addTopRule(s, t.accent());
        addText(s, slide.title(), 68, 48, 880, 62, 30D, true, t.primary());
        addText(s, text(slide.takeaway(), ""), 72, 112, 860, 42, 18D, true, t.secondary());
        List<AiPptContentBlock> blocks = blocks(slide).stream().limit(limit).toList();
        for (int i = 0; i < blocks.size(); i++) {
            int x = 95 + i * 375;
            addPanel(s, x, 205, 300, 300, i == 1 ? t.panel() : Color.WHITE);
            addText(s, "%02d".formatted(i + 1), x + 30, 238, 80, 42, 28D, true, t.accent());
            addText(s, text(blocks.get(i).title(), blocks.get(i).text()), x + 30, 305, 240, 56, 22D, true, t.primary());
            addText(s, text(blocks.get(i).text(), ""), x + 30, 372, 235, 86, 15D, false, t.text());
        }
    }

    private void renderCompare(XMLSlideShow ppt, AiPptDeckPlan deckPlan, AiPptSlidePlan slide) {
        XSLFSlide s = ppt.createSlide();
        Theme t = theme(deckPlan);
        addBackground(s, t.surface());
        addTopRule(s, t.accent());
        addText(s, slide.title(), 68, 48, 900, 62, 30D, true, t.primary());
        addText(s, text(slide.takeaway(), ""), 76, 118, 940, 42, 18D, true, t.secondary());
        addPanel(s, 86, 198, 510, 360, Color.WHITE);
        addPanel(s, 675, 198, 510, 360, t.panel());
        addText(s, "当前 / 问题", 116, 228, 420, 34, 22D, true, t.primary());
        addText(s, "目标 / 方案", 705, 228, 420, 34, 22D, true, t.accent());
        List<AiPptContentBlock> blocks = blocks(slide);
        addBullets(s, blocks.stream().limit(3).toList(), 124, 290, 420, 220, 18D, t.text());
        addBullets(s, blocks.stream().skip(3).limit(3).toList(), 713, 290, 420, 220, 18D, t.text());
    }

    private void renderTimeline(XMLSlideShow ppt, AiPptDeckPlan deckPlan, AiPptSlidePlan slide) {
        XSLFSlide s = ppt.createSlide();
        Theme t = theme(deckPlan);
        addBackground(s, t.surface());
        addTopRule(s, t.accent());
        addText(s, slide.title(), 68, 48, 850, 62, 30D, true, t.primary());
        addText(s, text(slide.takeaway(), ""), 70, 112, 860, 36, 18D, true, t.secondary());
        List<AiPptContentBlock> blocks = blocks(slide).stream().limit(4).toList();
        for (int i = 0; i < blocks.size(); i++) {
            int x = 110 + i * 275;
            addAccent(s, x, 265, 88, 88, t.accent(), 0.08);
            addText(s, "%02d".formatted(i + 1), x + 20, 284, 70, 40, 24D, true, t.accent());
            addText(s, text(blocks.get(i).title(), blocks.get(i).text()), x - 28, 382, 185, 94, 17D, false, t.text());
        }
    }

    private void addBackground(XSLFSlide slide, Color color) {
        XSLFAutoShape bg = slide.createAutoShape();
        bg.setShapeType(ShapeType.RECT);
        bg.setAnchor(new Rectangle(0, 0, 1280, 720));
        bg.setFillColor(color);
        bg.setLineColor(color);
    }

    private void addTopRule(XSLFSlide slide, Color color) {
        XSLFAutoShape rule = slide.createAutoShape();
        rule.setShapeType(ShapeType.RECT);
        rule.setAnchor(new Rectangle(0, 0, 1280, 12));
        rule.setFillColor(color);
        rule.setLineColor(color);
    }

    private void addPanel(XSLFSlide slide, int x, int y, int w, int h, Color color) {
        XSLFAutoShape panel = slide.createAutoShape();
        panel.setShapeType(ShapeType.ROUND_RECT);
        panel.setAnchor(new Rectangle(x, y, w, h));
        panel.setFillColor(color);
        panel.setLineColor(color);
    }

    private void addAccent(XSLFSlide slide, int x, int y, int w, int h, Color color, double alpha) {
        XSLFAutoShape shape = slide.createAutoShape();
        shape.setShapeType(ShapeType.ELLIPSE);
        shape.setAnchor(new Rectangle(x, y, w, h));
        Color mixed = mix(color, Color.WHITE, alpha);
        shape.setFillColor(mixed);
        shape.setLineColor(mixed);
    }

    private void addText(XSLFSlide slide, String text, int x, int y, int w, int h, Double size, boolean bold, Color color) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(x, y, w, h));
        XSLFTextParagraph p = box.addNewTextParagraph();
        XSLFTextRun run = p.addNewTextRun();
        run.setText(text(text, ""));
        run.setFontFamily("Microsoft YaHei");
        run.setFontSize(size);
        run.setBold(bold);
        run.setFontColor(color);
    }

    private void addBullets(XSLFSlide slide, List<AiPptContentBlock> blocks, int x, int y, int w, int h, Double size, Color color) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(x, y, w, h));
        for (AiPptContentBlock block : blocks) {
            XSLFTextParagraph p = box.addNewTextParagraph();
            p.setBullet(true);
            p.setLeftMargin(28D);
            p.setIndent(-18D);
            XSLFTextRun run = p.addNewTextRun();
            run.setText(text(block.title(), block.text()));
            run.setFontFamily("Microsoft YaHei");
            run.setFontSize(size);
            run.setFontColor(color);
        }
    }

    private List<AiPptContentBlock> blocks(AiPptSlidePlan slide) {
        if (slide.content() == null || slide.content().isEmpty()) {
            return List.of(new AiPptContentBlock("point", "核心要点", text(slide.takeaway(), "补充关键观点。"), null, null, null));
        }
        return slide.content();
    }

    private String firstContent(AiPptSlidePlan slide) {
        return blocks(slide).isEmpty() ? "" : text(blocks(slide).get(0).text(), blocks(slide).get(0).title());
    }

    private Theme theme(AiPptDeckPlan deckPlan) {
        AiPptTheme theme = deckPlan.theme();
        return new Theme(
                color(theme == null ? null : theme.primaryColor(), new Color(18, 34, 55)),
                color(theme == null ? null : theme.secondaryColor(), new Color(93, 116, 145)),
                color(theme == null ? null : theme.accentColor(), new Color(219, 118, 64)),
                color(theme == null ? null : theme.backgroundColor(), new Color(250, 251, 252)),
                color(theme == null ? null : theme.textColor(), new Color(31, 41, 55)),
                new Color(241, 245, 249)
        );
    }

    private Color color(String hex, Color fallback) {
        if (!StringUtils.hasText(hex)) {
            return fallback;
        }
        try {
            return Color.decode(hex);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private Color mix(Color base, Color overlay, double overlayWeight) {
        double baseWeight = 1D - overlayWeight;
        return new Color(
                Math.min(255, (int) Math.round(base.getRed() * baseWeight + overlay.getRed() * overlayWeight)),
                Math.min(255, (int) Math.round(base.getGreen() * baseWeight + overlay.getGreen() * overlayWeight)),
                Math.min(255, (int) Math.round(base.getBlue() * baseWeight + overlay.getBlue() * overlayWeight))
        );
    }

    private String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private AiPptDeckPlan emptyDeck() {
        return new AiPptDeckPlan(new DeckMeta("AI PPT", null, null, null, "zh-CN", null, 1, null, null, null), new AiPptTheme("#122237", "#5D7491", "#DB7640", "#FAFBFC", "#1F2937", "Microsoft YaHei", "consulting-clean"), List.of(), List.of(new AiPptSlidePlan("slide_01", 1, "cover", "cover", "AI PPT", null, "", List.of(), new AiPptLayoutSpec("cover-hero", null, "medium", null), new AiPptVisualSpec(List.of(), null, null, null, null), null)));
    }

    private record Theme(Color primary, Color secondary, Color accent, Color surface, Color text, Color panel) {}
}
