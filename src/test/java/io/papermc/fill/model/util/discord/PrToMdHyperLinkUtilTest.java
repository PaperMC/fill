package io.papermc.fill.model.util.discord;

import io.papermc.fill.util.discord.PrToMdHyperLinkUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrToMdHyperLinkUtilTest {
  @ParameterizedTest
  @ValueSource(strings = {
    "no mention",
    "(#36) not trailing",
    "missing space(#72)",
    "not in brackets #72",
    ""
  })
  void returnsUnchangedForInvalidCases(String commitSummary) {
    String actual = PrToMdHyperLinkUtil.hyperlinkTrailingPrMention(commitSummary, "myOrg", "myRepo");
    assertEquals(commitSummary, actual);
  }

  @ParameterizedTest
  @CsvSource({
    "Rework broken xyz (#123), myOrg, myRepo, Rework broken xyz ([#123](https://github.com/myOrg/myRepo/pull/123))",
    "Don't run player loot table for spectators (#11801), PaperMC, Paper, Don't run player loot table for spectators ([#11801](https://github.com/PaperMC/Paper/pull/11801))",
    "feat: add new xyz API   (#12), myOrg, myRepo, feat: add new xyz API   ([#12](https://github.com/myOrg/myRepo/pull/12))",
    "Fix (#1234) via xyz (#4125), myOrg, myRepo, Fix (#1234) via xyz ([#4125](https://github.com/myOrg/myRepo/pull/4125))"
  })
  void replacesTrailingPrMentionWithMarkdownLink(String commitSummary, String repoOwner, String repoName, String expected) {
    String actual = PrToMdHyperLinkUtil.hyperlinkTrailingPrMention(commitSummary, repoOwner, repoName);
    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @CsvSource({
    "commitSummary, null, myOrg, myRepo",
    "repoOwner, working summary (#1), null, myRepo",
    "repoName, working summary (#1), myOrg, null"
  })
  @SuppressWarnings("DataFlowIssue")
  void nullParameterThrowsNPE(String parameter, String commitSummary, String repoOwner, String repoName) {
    assertThrows(NullPointerException.class, () ->
        PrToMdHyperLinkUtil.hyperlinkTrailingPrMention(
          "null".equals(commitSummary) ? null : commitSummary,
          "null".equals(repoOwner) ? null : repoOwner,
          "null".equals(repoName) ? null : repoName
        ),
      () -> "Expected NullPointerException for parameter " + parameter
    );
  }
}
