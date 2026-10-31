package com.github.victormpcmun.bookmark2html.render;

import com.github.victormpcmun.bookmark2html.favicon.FaviconSource;
import com.github.victormpcmun.bookmark2html.model.BookmarkFolder;
import com.github.victormpcmun.bookmark2html.model.BookmarkLink;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Renders a folder as a complete, self-contained html page: stylesheet and favicons embedded.
 * The favicon of the page itself, when there is one, is only linked by its url.
 */
public class PrettyHtmlRenderer implements BookmarkRenderer {

    private static final String TEMPLATE_RESOURCE = "template.html";
    private static final String STYLE_RESOURCE = "style.css";

    /** Placed right after the viewport meta tag, hence the leading line break. */
    private static final String PAGE_FAVICON = "\n<link rel=\"icon\" href=\"%s\">";

    private final PageTemplate template;
    private final String style;
    private final FaviconSource faviconSource;
    private final Optional<String> pageFavicon;
    private final Clock clock;

    public PrettyHtmlRenderer(PageTemplate template, String style, FaviconSource faviconSource,
                              Optional<String> pageFavicon, Clock clock) {
        this.template = template;
        this.style = style;
        this.faviconSource = faviconSource;
        this.pageFavicon = pageFavicon;
        this.clock = clock;
    }

    public static PrettyHtmlRenderer withDefaultTemplate(FaviconSource faviconSource, Optional<String> pageFavicon) {
        return new PrettyHtmlRenderer(
                new PageTemplate(Resources.read(TEMPLATE_RESOURCE)),
                Resources.read(STYLE_RESOURCE),
                faviconSource,
                pageFavicon,
                Clock.systemDefaultZone());
    }

    @Override
    public String render(BookmarkFolder folder) {
        return template.fill(Map.of(
                "title", Html.escape(folder.name()),
                "pageFavicon", pageFavicon.map(url -> PAGE_FAVICON.formatted(Html.escape(url))).orElse(""),
                "summary", summary(folder),
                "style", style,
                "content", content(folder),
                "date", LocalDate.now(clock).toString()));
    }

    private String content(BookmarkFolder folder) {
        Map<String, String> favicons = faviconSource.faviconsFor(domainsOf(folder));
        return new BookmarkTreeHtml(favicons).render(folder);
    }

    private Set<String> domainsOf(BookmarkFolder folder) {
        return folder.allLinks().stream()
                .map(BookmarkLink::domain)
                .filter(domain -> !domain.isEmpty())
                .collect(Collectors.toSet());
    }

    private String summary(BookmarkFolder folder) {
        return plural(folder.countLinks(), "bookmark") + " &middot; " + plural(folder.countSubfolders(), "folder");
    }

    private static String plural(long count, String word) {
        return count + " " + word + (count == 1 ? "" : "s");
    }
}
