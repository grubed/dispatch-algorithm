package com.mrwind.dispatch.algorithm.pointgroup;

import com.mrwind.dispatch.algorithm.common.Point;

import java.util.ArrayList;

public class TreeNode {

    public interface TraversalCallback {
        void onVisit(TreeNode node);
    }

    public final int index;
    public final Point point;
    public TreeNode parent;
    // 欧拉平面上的最小生成树 顶点度数不可能超过6 基本上不超过 4
    public final ArrayList<TreeNode> children = new ArrayList<>(3);
    // 以该节点为根节点的子树的节点数量
    public int nodeCount;
    // 以该节点为根节点的子树的所有顶点权重和
    public float nodeWeight;
    // 以该节点为根节点的子树的所有边权重和
    public int edgeWeight;

    public TreeNode(int index, Point point) {
        this.index = index;
        this.point = point;
        this.nodeCount = 1;
        this.nodeWeight = point.weight;
    }

    /**
     * 添加子节点
     *
     * @param child      子节点
     * @param edgeWeight 当前节点到子节点的边的 edgeWeight
     */
    public void add(TreeNode child, int edgeWeight) {
        child.parent = this;
        children.add(child);
        updateWeight(child.nodeCount, child.edgeWeight + edgeWeight, child.nodeWeight);
    }

    public void remove(TreeNode child, int edgeWeight) {
        assert child.parent == this;
        child.parent = null;
        children.remove(child);
        updateWeight(-(child.nodeCount), -(child.edgeWeight + edgeWeight), -child.nodeWeight);
    }

    public int getChildCount() {
        return children.size();
    }

    // 更新插入子树引起的 nodeCount edgeWeight 变化
    private void updateWeight(int dNodeCount, int dEdgeWeight, float dNodeWeight) {
        nodeCount += dNodeCount;
        edgeWeight += dEdgeWeight;
        nodeWeight += dNodeWeight;
        if (parent != null) {
            parent.updateWeight(dNodeCount, dEdgeWeight, dNodeWeight);
        }
    }

    public void preOrderTraversal(TraversalCallback callback) {
        callback.onVisit(this);
        for (TreeNode child : children) {
            child.preOrderTraversal(callback);
        }
    }

    public TreeNode getRoot() {
        TreeNode root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        return root;
    }

    @Override
    public String toString() {
        return "TreeNode{" +
                "index: " + index +
                ", " + point +
                ", childCount: " + children.size() +
                ", nodeCount: " + nodeCount +
                ", nodeWeight: " + nodeWeight +
                ", edgeWeight: " + edgeWeight +
                ", pi: " + (parent == null ? -1 : parent.index) +
                ", root: " + (parent == null) +
                '}';
    }

    public String toPreOrderTraversalString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tree [\n");
        preOrderTraversal(node -> {
            sb.append(node);
            sb.append("\n");
        });
        sb.append("]");
        return sb.toString();
    }
}
