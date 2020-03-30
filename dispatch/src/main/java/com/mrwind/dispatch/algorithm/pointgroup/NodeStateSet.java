package com.mrwind.dispatch.algorithm.pointgroup;

class NodeStateSet implements TreeNode.TraversalCallback {

    static class PendingCut {
        TreeNode subTree;
        TreeNode parentTree;
        Edge edge;
        // 两棵树的和 去掉了断边
        float totalWeight;
        // 两棵树的方差
        float variance;
    }

    static private PendingCut GROUPED = new PendingCut();

    // 节点状态标记 null 代表未分割 GROUPED 代表已分割 其它指向待确认
    private PendingCut[] set;
    private PendingCut markState;

    NodeStateSet(int count) {
        set = new PendingCut[count];
    }

    // 标记已分割
    void markGrouped(TreeNode tree) {
        markState = GROUPED;
        tree.preOrderTraversal(this);
    }

    // 标记等待分割
    void markPending(TreeNode tree, PendingCut pendingCut) {
        markState = pendingCut;
        tree.preOrderTraversal(this);
    }

    void markUnGroup(TreeNode tree) {
        markState = null;
        tree.preOrderTraversal(this);
    }

    boolean isGrouped(int index) {
        return set[index] == GROUPED;
    }

    PendingCut getState(int index) {
        return set[index];
    }

    boolean isPending(PendingCut pendingCut) {
        return pendingCut != null && pendingCut != GROUPED;
    }

    @Override
    public void onVisit(TreeNode node) {
        set[node.index] = markState;
    }
}