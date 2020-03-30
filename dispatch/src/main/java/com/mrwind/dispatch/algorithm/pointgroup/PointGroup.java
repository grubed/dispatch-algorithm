package com.mrwind.dispatch.algorithm.pointgroup;

import com.mrwind.dispatch.algorithm.common.CoordinateUtils;
import com.mrwind.dispatch.algorithm.common.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PointGroup {

    public static class Builder {
        private PointGroup pointGroup;
        private PointGroup oldPointGroup;

        public Builder() {
            pointGroup = new PointGroup();
        }

        Builder(PointGroup oldPointGroup) {
            pointGroup = new PointGroup();
            this.oldPointGroup = oldPointGroup;

            pointGroup.minPointCount = oldPointGroup.minPointCount;
            pointGroup.maxPointCount = oldPointGroup.maxPointCount;
            pointGroup.overflowPointFactor = oldPointGroup.overflowPointFactor;
            pointGroup.distanceWeight = oldPointGroup.distanceWeight;
            pointGroup.minWeight = oldPointGroup.minWeight;
            pointGroup.maxWeight = oldPointGroup.maxWeight;
            pointGroup.limitWeight = oldPointGroup.limitWeight;
            pointGroup.rightAngleTreeWeight = oldPointGroup.rightAngleTreeWeight;
            pointGroup.rightAngleMST = oldPointGroup.rightAngleMST;
            pointGroup.points = oldPointGroup.points;
            pointGroup.coordType = oldPointGroup.coordType;
            pointGroup.isCoordTransformed = oldPointGroup.isCoordTransformed;
        }

        public Builder minPointCount(int minPointCount) {
            pointGroup.minPointCount = minPointCount;
            return this;
        }

        public Builder maxPointCount(int maxPointCount) {
            pointGroup.maxPointCount = maxPointCount;
            return this;
        }

        public Builder overflowPointFactor(float overflowPointFactor) {
            pointGroup.overflowPointFactor = overflowPointFactor;
            return this;
        }

        public Builder distanceWeight(float distanceWeight) {
            pointGroup.distanceWeight = distanceWeight;
            return this;
        }

        public Builder minWeight(float minWeight) {
            pointGroup.minWeight = minWeight;
            return this;
        }

        public Builder maxWeight(float maxWeight) {
            pointGroup.maxWeight = maxWeight;
            return this;
        }

        public Builder limitWeight(float limitWeight) {
            pointGroup.limitWeight = limitWeight;
            return this;
        }

        public Builder rightAngleTreeWeight(boolean rightAngleTreeWeight) {
            pointGroup.rightAngleTreeWeight = rightAngleTreeWeight;
            return this;
        }

        public Builder rightAngleMST(boolean rightAngleMST) {
            pointGroup.rightAngleMST = rightAngleMST;
            return this;
        }

        public Builder points(List<Point> points, int coordType) {
            pointGroup.points = points;
            pointGroup.coordType = coordType;
            pointGroup.isCoordTransformed = false;
            return this;
        }

        public PointGroup build() {

            if (pointGroup.maxPointCount > 0 && pointGroup.minPointCount == 0) {
                pointGroup.minPointCount = pointGroup.maxPointCount / 2;
            }
            if (pointGroup.maxWeight > 0 && pointGroup.minWeight == 0) {
                pointGroup.minWeight = pointGroup.maxWeight / 2;
            }

            if (pointGroup.rightAngleMST && !pointGroup.rightAngleTreeWeight) {
                throw new Error("rightAngleMST 开启时 rightAngleTreeWeight 必须也为true");
            }

            if (oldPointGroup != null) {
                if (oldPointGroup.points == pointGroup.points
                        && oldPointGroup.rightAngleMST == pointGroup.rightAngleMST
                        && oldPointGroup.rightAngleTreeWeight == pointGroup.rightAngleTreeWeight) {
                    pointGroup.graph = oldPointGroup.graph;
                    pointGroup.mst = oldPointGroup.mst;
                }
            }

            return pointGroup;
        }
    }

    // google 地图坐标 暂时 gcj02 也用这个
    public static final int COORDINATE_WGS84 = CoordinateUtils.COORDINATE_WGS84;
    // 高斯平面坐标
    public static final int COORDINATE_GAUSS = CoordinateUtils.COORDINATE_GAUSS;

    // 最小点数
    private int minPointCount;
    // 最大点数
    private int maxPointCount;
    // 超过最大点数量时整体权重的因子
    private float overflowPointFactor = 1;
    // 距离权重
    private float distanceWeight;
    // 最小权重
    private float minWeight;
    // 最大权重
    private float maxWeight;
    // 极限权重
    private float limitWeight;
    // 树的距离使用直角距离 (最小生成树以及边的排序还是直线距离)
    private boolean rightAngleTreeWeight = true;
    // 最小生成树也使用直角距离
    private boolean rightAngleMST;
    private List<Point> points;
    private int coordType;
    private boolean isCoordTransformed;

    private Graph graph;
    private int[] mst;


    private PointGroup() {
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public static void transformPoints(List<Point> points, int coordType) {
        if (coordType == COORDINATE_GAUSS) {
            return;
        }
        double[] xy;
        for (Point point : points) {
            xy = CoordinateUtils.geodetic2Gauss(point.lat, point.lng, 3, COORDINATE_WGS84);
            // 不需要 double 精度 int 就行(精确到米)
            point.x = Math.round(xy[0]);
            point.y = Math.round(xy[1]);
        }
    }

    public Response group() {
        if (!isCoordTransformed) {
            transformPoints(points, coordType);
            isCoordTransformed = true;
        }

        if (graph == null) {
            // 计算距离 生成完全无向图
            graph = new Graph(points, rightAngleMST);
//            System.out.println(graph);
//            System.out.println();

            // Prim 计算最小生成树 两点距离使用直线距离
            PrimMST primMST = new PrimMST();
            mst = primMST.primMST(graph.graph, graph.getPointCount(), 0);
//            primMST.printMST(mst, graph.getPointCount(), graph.graph);
//            System.out.println();
        }


        // 将数组简单表示的最小生成树转为双向树
        TreeGraph treeGraph = new TreeGraph(mst, graph, rightAngleTreeWeight);
//        System.out.println(treeGraph);

//        final int[] maxChildCount = {Integer.MIN_VALUE};
//        final int[] g2count = {0};
//        treeGraph.root.preOrderTraversal(node -> {
//            if (node.getChildCount() > maxChildCount[0]) {
//                maxChildCount[0] = node.getChildCount();
//            }
//            if (node.getChildCount() > 2) {
//                g2count[0]++;
//            }
//        });
//        System.out.println("maxChildCount: " + maxChildCount[0] + " g2count: " + g2count[0]);
//        System.out.println();

        Response response = treeGroup(treeGraph);
        response.graph = graph;
        response.mst = mst;

        return response;
    }

    /**
     * 计算点数和距离的综合权重
     */
    public float getWeight(int pointCount, float pointWeight, int distance) {
        float result = pointWeight + distanceWeight * distance;
        if (maxPointCount > 0 && pointCount > maxPointCount) {
            // 超过 maxPointCount
            float ratio = ((float) pointCount / maxPointCount);
            result *= ratio * ratio * overflowPointFactor;
        }

//        System.out.println("getWeight pointCount: " + pointCount + " pointWeight: " + pointWeight + " distance: " + distance + " result: " + result);

        return result;
    }

    public Response treeGroup(TreeGraph treeGraph) {
        int nodeCount = treeGraph.root.nodeCount;
        float minWeight = this.minWeight;
        float maxWeight = this.maxWeight;
        List<TreeNode> groupedTrees = new ArrayList<>();
        List<Edge> cutEdges = new ArrayList<>();
        Response response = new Response();
        response.groupedTrees = groupedTrees;
        response.cutEdges = cutEdges;

        if (getWeight(nodeCount, treeGraph.root.nodeWeight, treeGraph.root.edgeWeight) <= maxWeight) {
            // 整棵树本身就满足或者大小不够
            groupedTrees.add(treeGraph.root);
            return response;
        }

        // 边从大到小排序
        Arrays.sort(treeGraph.edges, (e1, e2) -> {
            if (rightAngleMST) {
                return e2.rightAngleWeight - e1.rightAngleWeight;
            }
            return e2.weight - e1.weight;
        });
        Edge[] sortedEdges = treeGraph.edges;
//        System.out.println("group sortedEdges: " + Arrays.toString(sortedEdges));
        NodeStateSet nodeStateSet = new NodeStateSet(nodeCount);
        int groupedNodeCount = 0;

        // 分割出来的子树
        TreeNode subTree;
        // 分割出来的子树 原来的根
        TreeNode parentTree;
        float subTreeWeight;
        float parentTreeWeight;

        for (Edge edge : sortedEdges) {
            assert nodeStateSet.getState(edge.src) == nodeStateSet.getState(edge.dest);
            if (nodeStateSet.isGrouped(edge.src)) {
                // 该边在剪出来的已符合要求的子树上
                continue;
            }

            subTree = treeGraph.getSubTreeByEdge(edge);
            parentTree = subTree.getRoot();
            subTreeWeight = getTreeWeight(subTree);
            parentTreeWeight = getParentTreeWeight(parentTree, subTree, edge);

//            System.out.println("edge: " + edge.src + " " + edge.dest + " parentTree.index: " + parentTree.index + " subTree.index: " + subTree.index
//                    + " subTreeWeight: " + subTreeWeight + " parentTreeWeight: " + parentTreeWeight);

            if (subTreeWeight >= minWeight && parentTreeWeight >= minWeight) {
                boolean isPendingTree = nodeStateSet.getState(edge.src) != null;

                // 两棵树有满足的 或者都过大 都需要分开
                cutTree(subTree, edge);
                cutEdges.add(edge);

                if (subTreeWeight <= maxWeight) {
                    nodeStateSet.markGrouped(subTree);
                    groupedNodeCount += subTree.nodeCount;
                    groupedTrees.add(subTree);
                } else if (isPendingTree) {
                    // 如果原来已经被标记为 pending 则取消
                    nodeStateSet.markUnGroup(subTree);
                }

                if (parentTreeWeight <= maxWeight) {
                    nodeStateSet.markGrouped(parentTree);
                    groupedNodeCount += parentTree.nodeCount;
                    groupedTrees.add(parentTree);
                } else if (isPendingTree) {
                    nodeStateSet.markUnGroup(parentTree);
                }
            } else {
                // 至少一棵树小于

                float totalWeight = parentTreeWeight + subTreeWeight;
                float variance = getWeightVariance(parentTree, subTree, parentTreeWeight, subTreeWeight, totalWeight);
//                System.out.println("pendingCut union weight: " + getTreeWeight(parentTree)
//                        + " totalWeight: " + totalWeight + " variance: " + variance
//                        + " subTreeNodeCount: " + subTree.nodeCount + " totalNodeCount: " + parentTree.nodeCount
//                        + " edge.edgeWeight: " + edge.edgeWeight + " edge.rightAngleWeight: " + edge.rightAngleWeight
//                        + " parentTree.edgeWeight: " + parentTree.edgeWeight + " subTree.edgeWeight: " + subTree.edgeWeight
//                );

                NodeStateSet.PendingCut pendingCut = nodeStateSet.getState(edge.src);
                if (pendingCut != null) {
                    // 已经标记过待确认 看是否分割的比原来更好
                    // 看是否分割的比原来更平均 比较方差
                    // 因为原来的是切了一条较长的边 所以新的必须必原来更好才行 所以乘了 factor
                    float factor = totalWeight / pendingCut.totalWeight;
                    // 因为 overflowPointFactor 的存在 虽然截取的边越来越短但 totalWeight 还是可能变小
                    if (factor < 1.05f) {
                        factor = 1.05f;
                    }
                    if (variance * factor < pendingCut.variance) {
//                        System.out.println("update pendingCut xxxxx");
                        pendingCut.subTree = subTree;
                        pendingCut.totalWeight = totalWeight;
                        pendingCut.variance = variance;
                        pendingCut.edge = edge;
                    }
                } else {
                    pendingCut = new NodeStateSet.PendingCut();
                    pendingCut.subTree = subTree;
                    pendingCut.parentTree = parentTree;
                    pendingCut.totalWeight = totalWeight;
                    pendingCut.variance = variance;
                    pendingCut.edge = edge;
                    nodeStateSet.markPending(parentTree, pendingCut);
                }
            }

            if (groupedNodeCount == nodeCount) {
                break;
            }
        }

        if (groupedNodeCount != nodeCount) {
//            System.out.println("cut pending groupedNodeCount:" + groupedNodeCount);
            // 说明有 pending 或 不满足条件但已没有边可以切割的

            for (int index = 0; index < nodeCount; ++index) {
                NodeStateSet.PendingCut pendingCut = nodeStateSet.getState(index);
                if (pendingCut == null) {
                    // 被分割之后分为一包满足 一包大于 但已经没有边可以切割这棵树了
                    TreeNode failTree = treeGraph.nodes[index].getRoot();
                    nodeStateSet.markGrouped(failTree);
                    groupedNodeCount += failTree.nodeCount;
                    groupedTrees.add(failTree);

//                    System.out.println("failTree" + index + " root: " + failTree.index);
                } else if (nodeStateSet.isPending(pendingCut) && pendingCut.subTree.parent != null) {
//                    System.out.println("pendingCut: subTree: " + pendingCut.subTree + " parentTree: " + pendingCut.parentTree);
//                    System.out.println("pendingCut variance: " + pendingCut.variance + " subWeight: " + getTreeWeight(pendingCut.subTree) + " totalWeight: " + pendingCut.totalWeight + " union edgeWeight: " + getTreeWeight(pendingCut.parentTree));
                    // 分割 pending
                    groupedNodeCount += pendingCut.parentTree.nodeCount;

                    // 判断是否有必要分割 不超过 limitWeight 就不分
                    if (getTreeWeight(pendingCut.parentTree) < limitWeight) {
                        groupedTrees.add(pendingCut.parentTree);
                        nodeStateSet.markGrouped(pendingCut.parentTree);
                    } else {
                        cutEdges.add(graph.createEdge(pendingCut.subTree.parent.index, pendingCut.subTree.index));
                        cutTree(pendingCut.subTree, pendingCut.edge);
                        groupedTrees.add(pendingCut.subTree);
                        groupedTrees.add(pendingCut.parentTree);
                    }
                }
            }

//            System.out.println("cut pending end groupedNodeCount:" + groupedNodeCount);
        }

        assert groupedNodeCount == nodeCount;

        return response;
    }

    private float getTreeWeight(TreeNode tree) {
        return getWeight(tree.nodeCount, tree.nodeWeight, tree.edgeWeight);
    }

    private float getParentTreeWeight(TreeNode parent, TreeNode sub, Edge edge) {
        int distance = parent.edgeWeight - sub.edgeWeight;
        if (rightAngleTreeWeight) {
            distance -= edge.rightAngleWeight;
        } else {
            distance -= edge.weight;
        }
        return getWeight(parent.nodeCount - sub.nodeCount, parent.nodeWeight - sub.nodeWeight, distance);
    }

    // 获取方差 subTree 还没切下来
    private float getWeightVariance(TreeNode parent, TreeNode sub, float parentTreeWeight, float subTreeWeight, float totalWeight) {
        float avg = totalWeight / 2;
        return (subTreeWeight - avg) * (subTreeWeight - avg) +
                (parentTreeWeight - avg) * (parentTreeWeight - avg);
    }

    private void cutTree(TreeNode subTree, Edge edge) {
        assert subTree.parent != null;
        subTree.parent.remove(subTree, rightAngleTreeWeight ? edge.rightAngleWeight : edge.weight);
    }
}
