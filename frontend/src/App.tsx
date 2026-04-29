import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from './layouts/app-layout';
import { FunctionConfigPage } from './pages/function-config/page';
import { FunctionModelBindingPage } from './pages/function-model-binding/page';
import { ModelServicePage } from './pages/model-service/page';
import { ParamTemplatePage } from './pages/param-template/page';
import { WorkbenchPage } from './pages/workbench/page';

const App = () => (
  <Routes>
    <Route element={<AppLayout />}>
      <Route path="/" element={<Navigate to="/workbench" replace />} />
      <Route path="/workbench" element={<WorkbenchPage />} />
      <Route path="/functions" element={<FunctionConfigPage />} />
      <Route path="/model-services" element={<ModelServicePage />} />
      <Route path="/function-model-bindings" element={<FunctionModelBindingPage />} />
      <Route path="/param-templates" element={<ParamTemplatePage />} />
    </Route>
  </Routes>
);

export default App;
