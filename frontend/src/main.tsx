import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App as AntApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

const theme = {
  token: {
    colorPrimary: '#0f766e',
    colorInfo: '#0f766e',
    colorSuccess: '#3f8f5d',
    colorWarning: '#c27b1c',
    colorError: '#b8493a',
    colorBgLayout: 'transparent',
    colorBgContainer: '#fffdf9',
    colorText: '#1e2934',
    colorBorderSecondary: '#eadfcb',
    borderRadius: 14,
    controlHeight: 32,
    controlHeightLG: 38,
    controlHeightSM: 24,
    fontSize: 13,
    fontSizeLG: 15,
    fontFamily: 'Aptos, "Segoe UI Variable", "Microsoft YaHei", sans-serif',
    boxShadowSecondary: '0 24px 60px rgba(37, 51, 66, 0.10)',
  },
};

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={zhCN} theme={theme}>
        <AntApp>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </AntApp>
      </ConfigProvider>
    </QueryClientProvider>
  </React.StrictMode>,
);
