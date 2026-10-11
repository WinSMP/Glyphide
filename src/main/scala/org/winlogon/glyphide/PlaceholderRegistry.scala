// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import java.util.logging.Logger

/** A handler the config enabled, together with the tokens to substitute for it. */
final case class ConfiguredPlaceholder(handler: PlaceholderHandler, tokens: List[String])

/** Joins the `placeholder.<id>` config sections with the handlers that implement them.
  *
  * Resolving once (rather than per message) keeps [[PlaceholderTransform]] free of config lookups
  * and makes the active placeholder set an explicit, ordered value instead of scattered config
  * fields.
  */
object PlaceholderRegistry {

    /** Declaration order fixes substitution order, independently of the order of the YAML keys. */
    private val handlers: List[PlaceholderHandler] =
        List(ItemPlaceholderHandler, HotbarPlaceholderHandler)

    /** The enabled placeholders, handler token defaults filled in, config problems reported.
      *
      * Disabled sections, sections for ids no handler implements, and sections nobody
      * configured are all simply absent from the result - an unimplemented placeholder must
      * never affect chat.
      */
    def resolve(config: Configuration, logger: Logger): List[ConfiguredPlaceholder] = {
        val enabled = handlers.flatMap(handler => resolveOne(handler, config.placeholderSections))

        warnAboutUnknownSections(config.placeholderSections, logger)
        warnAboutDuplicateTokens(enabled, logger)
        logger.info(
            s"Glyphide: ${enabled.size} placeholder(s) enabled: " +
                enabled.map(_.handler.id).mkString(", ")
        )

        enabled
    }

    private def resolveOne(
        handler: PlaceholderHandler,
        sections: Map[String, Configuration.PlaceholderSection]
    ): Option[ConfiguredPlaceholder] =
        sections.get(handler.id) match {
            // no section at all means the placeholder was never configured, so it stays off
            case Some(section) if section.enabled =>
                val tokens = section.tokens.getOrElse(handler.defaultTokens)
                Some(ConfiguredPlaceholder(handler, tokens))
            case _ => None
        }

    /** Reports enabled sections with no handler, which is either a typo or a placeholder from
      * a newer version of Glyphide than the one running.
      */
    private def warnAboutUnknownSections(
        sections: Map[String, Configuration.PlaceholderSection],
        logger: Logger
    ): Unit = {
        val known = handlers.map(_.id).toSet
        sections.foreach { (id, section) =>
            if (section.enabled && !known.contains(id))
                logger.warning(
                    s"Placeholder '$id' is enabled in config but not implemented; ignoring it."
                )
        }
    }

    /** Reports tokens claimed by two placeholders, where the winner would depend on substitution
      * order and would be impossible for a server admin to reason about.
      */
    private def warnAboutDuplicateTokens(
        enabled: List[ConfiguredPlaceholder],
        logger: Logger
    ): Unit =
        enabled
            .flatMap(placeholder =>
                placeholder.tokens.map(token => (token, placeholder.handler.id))
            )
            .groupMap((token, _) => token)((_, id) => id)
            .foreach { (token, claimants) =>
                if (claimants.size > 1)
                    logger.warning(
                        s"Token '$token' is claimed by ${claimants.mkString(", ")}; only the first"
                            + "will substitute it."
                    )
            }
}
