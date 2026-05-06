import { useEffect } from "react";

export default function BlurOnBlurOverlay() {
  useEffect(() => {
    const shells = () => Array.from(document.getElementsByClassName("viewer-shell"));

    const blur = () => shells().forEach((el) => el.classList.add("viewer-blurred"));
    const unblur = () => shells().forEach((el) => el.classList.remove("viewer-blurred"));

    const onVis = () => (document.visibilityState === "hidden" ? blur() : unblur());

    window.addEventListener("blur", blur);
    window.addEventListener("focus", unblur);
    document.addEventListener("visibilitychange", onVis);

    let devToolsBlurred = false;
    const devToolsHeuristic = () => {
      const threshold = 160;
      const widthGap = window.outerWidth - window.innerWidth;
      const heightGap = window.outerHeight - window.innerHeight;
      const open = widthGap > threshold || heightGap > threshold;
      if (open && !devToolsBlurred) {
        blur();
        devToolsBlurred = true;
      } else if (!open && devToolsBlurred) {
        unblur();
        devToolsBlurred = false;
      }
    };
    const interval = setInterval(devToolsHeuristic, 1000);

    return () => {
      window.removeEventListener("blur", blur);
      window.removeEventListener("focus", unblur);
      document.removeEventListener("visibilitychange", onVis);
      clearInterval(interval);
    };
  }, []);

  return null;
}
