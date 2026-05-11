import { useMemo, useState } from 'react';
import {
  AppstoreOutlined,
  BranchesOutlined,
  ClusterOutlined,
  MenuOutlined,
  MessageOutlined,
  NodeIndexOutlined,
  ThunderboltOutlined,
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
    key: '/call-records',
    icon: <ThunderboltOutlined />,
    label: '调用记录',
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
        <Sider width={264} className="app-sider" theme="light">
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
        {!screens.lg ? (
          <Header className="app-header app-header--workbench" style={{ background: token.colorBgLayout }}>
            <Button icon={<MenuOutlined />} shape="circle" onClick={() => setDrawerOpen(true)} />
          </Header>
        ) : null}
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
