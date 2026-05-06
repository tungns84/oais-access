import { Link, Route, Routes } from "react-router-dom";
import HomePage from "./pages/HomePage";
import ViewerPage from "./pages/ViewerPage";
import AboutPage from "./pages/AboutPage";
import ModeSelector from "./components/ModeSelector";

export default function App() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <Link to="/" className="brand">
          📄 OAIS Access — POC
        </Link>
        <nav>
          <Link to="/">Tài liệu</Link>
          <Link to="/about">OAIS Mapping</Link>
        </nav>
        <ModeSelector />
      </header>
      <main className="app-main">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/view/:id" element={<ViewerPage />} />
          <Route path="/about" element={<AboutPage />} />
        </Routes>
      </main>
    </div>
  );
}
