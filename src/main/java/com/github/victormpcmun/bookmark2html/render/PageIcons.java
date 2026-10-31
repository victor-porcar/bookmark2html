package com.github.victormpcmun.bookmark2html.render;

import java.util.Optional;

/**
 * Icons of the generated page itself (not those of its bookmarks): the favicon and the apple
 * touch icon, each one optional and given by its url exactly as it must appear in the page.
 */
public record PageIcons(Optional<String> favicon, Optional<String> appleTouchIcon) {

    public static PageIcons none() {
        return new PageIcons(Optional.empty(), Optional.empty());
    }
}
