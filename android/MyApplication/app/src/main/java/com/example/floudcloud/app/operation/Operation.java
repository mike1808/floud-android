package com.example.floudcloud.app.operation;

import com.example.floudcloud.app.utility.ProgressListener;

public interface Operation {
    public int execute(ProgressListener progressListener);
}