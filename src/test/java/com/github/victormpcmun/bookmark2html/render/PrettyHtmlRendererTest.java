package com.github.victormpcmun.bookmark2html.render;

import com.github.victormpcmun.bookmark2html.favicon.FaviconSource;
import com.github.victormpcmun.bookmark2html.model.BookmarkFolder;
import com.github.victormpcmun.bookmark2html.model.BookmarkLink;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrettyHtmlRendererTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC);
    private static final FaviconSource FAKE_FAVICONS = domains -> Map.of("oracle.com", "data:image/png;base64,AAAA");

    private final BookmarkFolder folder = new BookmarkFolder("Tech & Co", List.of(
            new BookmarkLink("Java <docs>", "https://www.oracle.com/java?a=1&b=2"),
            new BookmarkFolder("Tools", List.of(new BookmarkLink("", "https://github.com/")))));

    @Test
    void fillsTitleSummaryAndDate() {
        String html = render("{{title}}|{{summary}}|{{date}}");

        assertEquals("Tech &amp; Co|2 bookmarks &middot; 1 folder|2026-09-21", html);
    }

    @Test
    void escapesBookmarkTextAndUrls() {
        String html = render("{{content}}");

        assertTrue(html.contains("<span class=\"link-title\">Java &lt;docs&gt;</span>"));
        assertTrue(html.contains("href=\"https://www.oracle.com/java?a=1&amp;b=2\""));
        assertFalse(html.contains("<docs>"));
    }

    @Test
    void showsDomainAndFallsBackToUrlWhenNameIsEmpty() {
        String html = render("{{content}}");

        assertTrue(html.contains("<span class=\"link-domain\">oracle.com</span>"));
        assertTrue(html.contains("<span class=\"link-title\">https://github.com/</span>"));
    }

    @Test
    void embedsTheFaviconOfTheLinkDomain() {
        String html = render("{{content}}");

        assertTrue(html.contains("<img class=\"link-icon\" src=\"data:image/png;base64,AAAA\" alt=\"\">"));
    }

    @Test
    void leavesAnEmptySlotWhenThereIsNoFavicon() {
        String html = render("{{content}}");

        assertTrue(html.contains("<span class=\"link-icon\"></span>"));
    }

    @Test
    void rendersSubfoldersAsCollapsibleNestedTrees() {
        String html = render("{{content}}");

        assertTrue(html.contains("<li class=\"folder\" id=\"tools\">"));
        assertTrue(html.contains("<details open>"));
        assertTrue(html.contains("<span class=\"folder-name\">Tools</span>"));
    }

    @Test
    void anchorsEveryFolderWithTheNamesOfItsAncestors() {
        BookmarkFolder nested = new BookmarkFolder("Root", List.of(new BookmarkFolder("Java", List.of(
                new BookmarkFolder("Spring Boot", List.of(new BookmarkLink("Guide", "https://spring.io/")))))));

        String html = new PrettyHtmlRenderer(new PageTemplate("{{content}}"), "", FAKE_FAVICONS, Optional.empty(),
                FIXED_CLOCK).render(nested);

        assertTrue(html.contains("<li class=\"folder\" id=\"java/spring-boot\">"));
        assertTrue(html.contains("<a class=\"folder-anchor\" href=\"#java/spring-boot\""));
    }

    @Test
    void keepsTheOriginalOrderOfLinksAndFolders() {
        String html = render("{{content}}");

        assertTrue(html.indexOf("Java &lt;docs&gt;") < html.indexOf("Tools"));
        assertTrue(html.indexOf("Tools") < html.indexOf("https://github.com/"));
    }

    @Test
    void embedsTheStylesheet() {
        String html = new PrettyHtmlRenderer(new PageTemplate("{{style}}"), "body{}", FAKE_FAVICONS, Optional.empty(),
                FIXED_CLOCK).render(folder);

        assertEquals("body{}", html);
    }

    @Test
    void linksThePageFaviconOnTheNextLine() {
        String html = new PrettyHtmlRenderer(new PageTemplate("<meta>{{pageFavicon}}"), "", FAKE_FAVICONS,
                Optional.of("icons/a&b.png"), FIXED_CLOCK).render(folder);

        assertEquals("<meta>\n<link rel=\"icon\" href=\"icons/a&amp;b.png\">", html);
    }

    @Test
    void leavesThePageWithoutFaviconWhenThereIsNone() {
        assertEquals("<meta>", render("<meta>{{pageFavicon}}"));
    }

    private String render(String template) {
        return new PrettyHtmlRenderer(new PageTemplate(template), "", FAKE_FAVICONS, Optional.empty(), FIXED_CLOCK)
                .render(folder);
    }
}
