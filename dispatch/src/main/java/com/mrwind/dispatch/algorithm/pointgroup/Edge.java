package com.mrwind.dispatch.algorithm.pointgroup;

public class Edge {
    public int src;
    public int dest;
    // 直线距离
    public int weight;
    // 直角距离
    public int rightAngleWeight;

    public Edge(int src, int dest, int weight, int rightAngleWeight) {
        this.src = src;
        this.dest = dest;
        this.weight = weight;
        this.rightAngleWeight = rightAngleWeight;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "src=" + src +
                ", dest=" + dest +
                ", weight=" + weight +
                ", rightAngleWeight=" + rightAngleWeight +
                '}';
    }
}
