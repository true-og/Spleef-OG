package org.battleplugins.arena.spleef;

import org.battleplugins.arena.messages.Message;
import org.battleplugins.arena.messages.Messages;

public final class SpleefMessages {

    public static final Message LAYER_SET_MIN_POSITION = Messages.info("spleef-editor-layer-set-min-position",
            "Click a block to set the minimum (first) position of the layer.");
    public static final Message LAYER_SET_MAX_POSITION = Messages.info("spleef-editor-layer-set-max-position",
            "Click a block to set the maximum (second) position of the layer.");
    public static final Message LAYER_SET_BLOCK_DATA = Messages.info("spleef-editor-set-block-data",
            "Enter the name of the block the layer should be made of. Type \"cancel\" to cancel.");
    public static final Message LAYER_SET_BLOCK_DATA_INVALID = Messages.error("spleef-set-block-data-invalid",
            "Invalid block name. Please try again.");
    public static final Message DEATH_REGION_SET_MIN_POSITION = Messages.info(
            "spleef-editor-death-region-set-min-position",
            "Click a block to set the minimum (first) position of the death region.");
    public static final Message DEATH_REGION_SET_MAX_POSITION = Messages.info(
            "spleef-editor-death-region-set-max-position",
            "Click a block to set the maximum (second) position of the death region.");

    public static final Message LAYER_ADDED = Messages.success("spleef-layer-added", "Layer added successfully!");
    public static final Message LAYER_REMOVED = Messages.success("spleef-layer-removed", "Layer removed successfully!");
    public static final Message LAYER_REMOVED_ALL = Messages.success("spleef-layer-removed-all",
            "All layers removed successfully!");
    public static final Message LAYER_INDEX_CHANGED = Messages.success("spleef-layer-index-changed",
            "Layer index changed successfully!");
    public static final Message LAYER_INFO = Messages.info("spleef-layer-info",
            "Layer <secondary>#{}</secondary>: <secondary>Min:</secondary> <primary>{}</primary>, <secondary>Max:</secondary> <primary>{}</primary>, <secondary>Block:</secondary> <primary>{}</primary>");
    public static final Message INVALID_LAYER = Messages.error("spleef-invalid-layer",
            "Invalid layer! There are only <secondary>{}</secondary> layers.");
    public static final Message NO_LAYERS = Messages.error("spleef-no-layers", "There are no layers in this map!");
    public static final Message DEATH_REGION_SET = Messages.success("spleef-death-region-set",
            "Death region set successfully!");

    public static final Message CREATE_WORLD_NOT_WHITELISTED = Messages.error("spleef-create-world-not-whitelisted",
            "Spleef arenas cannot be created in this world. World <secondary>{}</secondary> is not in the Spleef world whitelist.");
    public static final Message CREATE_REGION_NOT_WHITELISTED = Messages.error("spleef-create-region-not-whitelisted",
            "Spleef arenas cannot be created here. Stand inside a WorldGuard region listed in the Spleef region whitelist.");
    public static final Message CREATE_WG_FLAG_REQUIRED = Messages.error("spleef-create-wg-flag-required",
            "You must stand inside a WorldGuard region that has the <secondary>allow-spleef</secondary> flag set to <secondary>allow</secondary> to create a Spleef arena here.");
    public static final Message CREATE_WG_MISSING = Messages.error("spleef-create-wg-missing",
            "WorldGuard is not loaded; Spleef arena creation is disabled.");
    public static final Message WORLD_BLOCKED_AT_RUNTIME = Messages.error("spleef-world-blocked",
            "Spleef matches are not permitted in world <secondary>{}</secondary>.");
    public static final Message MATCH_REGION_NOT_WHITELISTED = Messages.error("spleef-match-region-not-whitelisted",
            "This Spleef map is not inside a whitelisted WorldGuard region.");
    public static final Message WORLDGUARD_REGION_NOT_FOUND = Messages.error("spleef-worldguard-region-not-found",
            "WorldGuard region <secondary>{}</secondary> was not found in this map's world.");
    public static final Message WORLDGUARD_REGION_SET = Messages.success("spleef-worldguard-region-set",
            "WorldGuard region set to <secondary>{}</secondary>.");

}
