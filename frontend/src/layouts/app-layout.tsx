import { useMemo, useState } from 'react';
import {
  AppstoreOutlined,
  BranchesOutlined,
  ClusterOutlined,
  MenuOutlined,
  MessageOutlined,
  NodeIndexOutlined,
} from '@ant-design/icons';
import { Button, Drawer, Grid, Layout, Menu, Tag, theme } from 'antd';
import type { MenuProps } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

const { Header, Content, Sider } = Layout;

const menuItems: Required<MenuProps>['items'] = [
  {
    key: '/workbench',
    icon: <MessageOutlined />,
    label: '工作台',
  },
  {
    key: '/functions',
    icon: <AppstoreOutlined />,
    label: '功能配置',
  },
  {
    key: '/model-services',
    icon: <ClusterOutlined />,
    label: '模型服务',
  },
  {
    key: '/function-model-bindings',
    icon: <BranchesOutlined />,
    label: '功能绑定',
  },
  {
    key: '/param-templates',
    icon: <NodeIndexOutlined />,
    label: '参数模板',
  },
];

export const AppLayout = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const screens = Grid.useBreakpoint();
  const { token } = theme.useToken();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const isWorkbench = location.pathname.startsWith('/workbench');

  const selectedKey = useMemo(() => {
    const item = menuItems.find((entry) => typeof entry?.key === 'string' && location.pathname.startsWith(entry.key));
    return item?.key ? [String(item.key)] : ['/workbench'];
  }, [location.pathname]);

  const navigationMenu = (
    <Menu
      mode="inline"
      items={menuItems}
      selectedKeys={selectedKey}
      onClick={({ key }) => {
        navigate(String(key));
        setDrawerOpen(false);
      }}
    />
  );

  return (
    <Layout className="app-shell">
      {screens.lg ? (
        <Sider width={284} className="app-sider" theme="light">
          <div className="brand-panel">
            <div className="brand-mark">MM</div>
            <div>
              <h2>多模态入口</h2>
              <p>Multimodal App Entry</p>
            </div>
          </div>
          <div className="brand-note">
            <Tag color={isWorkbench ? 'cyan' : 'gold'}>{isWorkbench ? 'Stage 2' : 'Stage 1'}</Tag>
            <span>
              {isWorkbench
                ? '工作台基础壳子开发中，正在为任务主链路铺底。'
                : '配置中心 CRUD 已就绪，当前用于支撑工作台参数驱动。'}
            </span>
          </div>
          {navigationMenu}
        </Sider>
      ) : null}
      <Layout>
        <Header className="app-header" style={{ background: token.colorBgLayout }}>
          <div className="hero-band">
            <div className="hero-band__copy">
              {!screens.lg ? (
                <Button
                  icon={<MenuOutlined />}
                  shape="circle"
                  className="hero-band__menu-trigger"
                  onClick={() => setDrawerOpen(true)}
                />
              ) : null}
              <div>
                <span className="hero-band__eyebrow">{isWorkbench ? 'Shupai / Workbench' : 'Shupai / Control Deck'}</span>
                <h1>{isWorkbench ? '多模态工作台基础壳子' : '多模态入口配置中心'}</h1>
                <p>
                  {isWorkbench
                    ? '先完成左侧导航、消息流、输入区、功能条与参数快捷区，再把会话、消息和任务骨架接进来。'
                    : '先把功能、模型、绑定与模板四条配置链路打透，再进入聊天工作台与任务主链路。'}
                </p>
              </div>
            </div>
            <div className="hero-band__signal">
              <span>{isWorkbench ? 'Workbench' : 'Backend'}</span>
              <strong>{isWorkbench ? 'Shell ready for step 5 preparation' : 'Ready for frontend integration'}</strong>
              <small>
                {isWorkbench
                  ? '当前结果使用轻量 mock 演化，后续接入会话 / 消息 / 任务接口。'
                  : '默认代理 `/api` -> `http://127.0.0.1:8080`'}
              </small>
            </div>
          </div>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
      <Drawer title="应用导航" placement="left" open={drawerOpen} onClose={() => setDrawerOpen(false)} width={280}>
        <div className="brand-note brand-note--mobile">
          <Tag color={isWorkbench ? 'cyan' : 'gold'}>{isWorkbench ? 'Stage 2' : 'Stage 1'}</Tag>
          <span>{isWorkbench ? '工作台壳子开发' : '配置中心页面联调'}</span>
        </div>
        {navigationMenu}
      </Drawer>
    </Layout>
  );
};
