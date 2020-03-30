package com.mrwind.dispatch.algorithm.pointgroup;

/**
 * https://zh.wikipedia.org/zh-hans/%E6%99%AE%E6%9E%97%E5%A7%86%E7%AE%97%E6%B3%95
 */
public class PrimMST {
    // A utility function to find the vertex with minimum key
    // value, from the set of vertices not yet included in MST
    private int minKey(int[] key, boolean[] mstSet, int V) {
        // Initialize min value
        int min = Integer.MAX_VALUE, minIndex = -1;

        for (int v = 0; v < V; v++)
            if (!mstSet[v] && key[v] < min) {
                min = key[v];
                minIndex = v;
            }

        return minIndex;
    }

    /**
     * @param graph 距离矩阵
     * @param V 顶点数量
     * @param root 根节点索引
     */
    public int[] primMST(int[][] graph, int V, int root) {
        // Array to store constructed MST
        int parent[] = new int[V];

        // Key values used to pick minimum weight edge in cut
        int key[] = new int[V];

        // To represent set of vertices not yet included in MST
        boolean mstSet[] = new boolean[V];

        for (int i = 0; i < V; i++) {
            key[i] = Integer.MAX_VALUE;
            mstSet[i] = false;
        }

        int u = root;
        key[u] = 0;
        parent[u] = -1;
        mstSet[u] = true;

        // The MST will have V vertices
        for (int count = 0; ; count++) {
            // Update key value and parent index of the adjacent
            // vertices of the picked vertex. Consider only those
            // vertices which are not yet included in MST
            for (int v = 0; v < V; v++) {

                // graph[u][v] is non zero only for adjacent vertices of m
                // mstSet[v] is false for vertices not yet included in MST
                // Update the key only if graph[u][v] is smaller than key[v]
                if (graph[u][v] >= 0 && !mstSet[v] &&
                        graph[u][v] < key[v]) {
                    parent[v] = u;
                    key[v] = graph[u][v];
                }
            }

            if (count >= V - 2) {
                // 最后一个点没必要更新 mstSet
                break;
            }

            // Pick thd minimum key vertex from the set of vertices
            // not yet included in MST
            u = minKey(key, mstSet, V);

            // Add the picked vertex to the MST Set
            mstSet[u] = true;
        }

        return parent;
    }

    public void printMST(int[] parent, int n, int[][] graph) {
        System.out.println("Edge   Weight");
        for (int i = 0; i < n; i++) {
            if (parent[i] != -1) {
                System.out.println(parent[i] + " - " + i + "    " +
                        graph[i][parent[i]]);
            }
        }
    }
}

