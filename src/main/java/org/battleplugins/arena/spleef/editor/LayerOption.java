package org.battleplugins.arena.spleef.editor;

import org.battleplugins.arena.editor.type.EditorKey;

public enum LayerOption implements EditorKey {

    BLOCK_DATA("blockData");

    private final String key;

    LayerOption(String key) {

        this.key = key;

    }

    @Override
    public String getKey() {

        return this.key;

    }

}
