package com.mrwind.dispatch.algorithm.pointgroup;

import com.mrwind.dispatch.algorithm.common.Point;

import java.util.List;

public class TreeGraph {

    public TreeNode root;
    // 快速根据 index 定位 TreeNode
    public TreeNode[] nodes;
    public Edge[] edges;

    public TreeGraph(int[] tree, Graph graph, boolean rightAngleWeight) {
        // 将数组简单表示的单向树转为双向树
        int count = graph.getPointCount();
        List<Point> points = graph.points;
        nodes = new TreeNode[count];
        for (int i = 0; i < count; ++i) {
            nodes[i] = new TreeNode(i, points.get(i));
        }
        edges = new Edge[count - 1];
        int edgeIndex = 0;
        for (int i = 0; i < count; ++i) {
            int parentIndex = tree[i];
            if (parentIndex != -1) {
                // 这里保证了 edge 的 src 为 parent
                Edge edge = graph.createEdge(parentIndex, i);
                nodes[parentIndex].add(nodes[i], rightAngleWeight ? edge.rightAngleWeight : edge.weight);
                edges[edgeIndex++] = edge;
            } else {
                root = nodes[i];
            }
        }
    }

    public TreeNode getSubTreeByEdge(Edge edge) {
        TreeNode srcNode = nodes[edge.src];
        TreeNode destNode = nodes[edge.dest];
        if (destNode.parent == srcNode) {
            return destNode;
        }
        return srcNode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TreeGraph [\n");
        root.preOrderTraversal(node -> {
            sb.append(node);
            sb.append("\n");
        });
        sb.append("]");
        return sb.toString();
    }
}
