package io.papermc.fill.util.discord;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PrToMdHyperLinkUtil {
  private static final Pattern TRAILING_PR = Pattern.compile(" \\(#(\\d+)\\)\\z");

  private PrToMdHyperLinkUtil() {
  }


  public  static String hyperlinkTrailingPrMention(final String commitSummary, final String repoOwner, final String repoName) {
    Objects.requireNonNull(commitSummary);
    Objects.requireNonNull(repoOwner);
    Objects.requireNonNull(repoName);

    if (commitSummary.isEmpty()) return commitSummary;

    final Matcher matcher = TRAILING_PR.matcher(commitSummary);
    if (!matcher.find()) return commitSummary;

    final String prNumber = matcher.group(1);

    return matcher.replaceFirst(
      String.format(" ([#%s](https://github.com/%s/%s/pull/%s))",
        prNumber, repoOwner, repoName, prNumber));
  }
}
