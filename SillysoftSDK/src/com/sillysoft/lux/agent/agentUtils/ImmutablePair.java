package com.sillysoft.lux.agent.agentUtils;

class ImmutablePair<TL, TR> extends Pair {
    private final TL left;
    private final TR right;

    ImmutablePair(TL left, TR right) {
        this.left = left;
        this.right = right;
    }

    @Override
    TL getLeft() {
        return left;
    }

    @Override
    TR getRight() {
        return right;
    }
}
