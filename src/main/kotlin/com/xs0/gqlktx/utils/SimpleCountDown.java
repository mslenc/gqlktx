package com.xs0.gqlktx.utils;

public class SimpleCountDown {
    private final Runnable onFinish;
    private final Runnable topLevelDone;
    private int remain;

    public SimpleCountDown(Runnable onFinish) {
        this.onFinish = onFinish;
        this.topLevelDone = step();
    }

    public void stepCreationDone() {
        topLevelDone.run();
    }

    public Runnable step() {
        remain++;
        return new Runnable() {
            boolean executed;

            @Override
            public void run() {
                if (executed)
                    return;

                executed = true;
                if (--remain < 1) {
                    onFinish.run();
                }
            }
        };
    }
}
