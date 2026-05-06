package com.poc.oais.access.service;

import com.poc.oais.access.config.AccessProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class ModeResolver {

    private final AccessProperties props;

    public ModeResolver(AccessProperties props) {
        this.props = props;
    }

    /**
     * Effective anti-download mode for this request.
     * Honors header {@code X-Anti-Download-Mode} (1|2|3) for demo flexibility,
     * falls back to {@code oais.anti-download.mode} from application.yml.
     */
    public int effectiveMode(HttpServletRequest req) {
        if (req != null) {
            String fromQuery = req.getParameter("m");
            int parsed = parse(fromQuery);
            if (parsed > 0) return parsed;
            int fromHeader = parse(req.getHeader("X-Anti-Download-Mode"));
            if (fromHeader > 0) return fromHeader;
        }
        return props.antiDownload().mode();
    }

    private static int parse(String raw) {
        if (raw == null) return 0;
        try {
            int v = Integer.parseInt(raw.trim());
            return (v >= 1 && v <= 3) ? v : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
