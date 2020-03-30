package com.mrwind.dispatch.algorithm.pointgroup;

import com.mrwind.dispatch.algorithm.common.Distance;
import com.mrwind.dispatch.algorithm.common.Point;

import java.util.Arrays;
import java.util.List;

/**
 * 图的矩阵表示
 * graph[i][j] 代表 i 到 j 的 weight
 * -1 代表不连通或者 i == j
 */
public class Graph {

    final int[][] graph;
    final List<Point> points;
    boolean rightAngleMST;

    public Graph(List<Point> points, boolean rightAngleMST) {
        this.points = points;
        this.rightAngleMST = rightAngleMST;
        this.graph = this.createGraph(points);
    }

    private int[][] createGraph(List<Point> points) {
        int pointCount = points.size();
        int[][] graph = new int[pointCount][pointCount];
        int weight;
        for (int i = 0; i < pointCount; ++i) {
            graph[i][i] = -1;
            for (int j = i + 1; j < pointCount; ++j) {
                if (rightAngleMST) {
                    weight = Distance.getRightAngleDistanceInt(points.get(i), points.get(j));
                } else {
                    weight = Distance.getDistanceInt(points.get(i), points.get(j));
                }
                graph[i][j] = graph[j][i] = weight;
            }
        }
        return graph;
    }

    public Edge createEdge(int src, int dest) {
        int weight = getEdgeWeight(src, dest);
        int rightAngleWeight = rightAngleMST ? weight : getEdgeRightAngleWeight(src, dest);
        return new Edge(src, dest, weight, rightAngleWeight);
    }

    public int getEdgeWeight(int src, int dest) {
        return graph[src][dest];
    }

    public int getEdgeRightAngleWeight(int src, int dest) {
        return Distance.getRightAngleDistanceInt(points.get(src), points.get(dest));
    }

    public int getPointCount() {
        return points.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Graph[\n");
        for (int[] ints : graph) {
            sb.append(Arrays.toString(ints)).append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
