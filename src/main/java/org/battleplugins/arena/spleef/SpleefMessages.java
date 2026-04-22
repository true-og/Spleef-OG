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

}
