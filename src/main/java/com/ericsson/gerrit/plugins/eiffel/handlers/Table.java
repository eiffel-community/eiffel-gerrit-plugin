package com.ericsson.gerrit.plugins.eiffel.handlers;

public enum Table {
    SCS_TABLE("branch"), SCC_TABLE("changeId");

    final String keyName;

    Table(String keyValue) {
        this.keyName = keyValue;
    }

    public String getKeyName() {
        return this.keyName;
    }
}
