function getDistance(p1, p2) {
  const dx = p1.x - p2.x;
  const dy = p1.y - p2.y;
  return Math.round(Math.sqrt(dx * dx + dy * dy));
}

function getRightAngleDistance(p1, p2) {
  return Math.round((Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y)) * 0.95);
}

class TreeNode {
  static arrayToTree(treeArray, points) {
    const count = points.length;
    const nodes = new Array(count);
    for (let i = 0; i < count; ++i) {
      nodes[i] = new TreeNode(i, points[i]);
    }

    let roots = [];
    let parentIndex;
    for (let i = 0; i < count; ++i) {
      parentIndex = treeArray[i];
      if (parentIndex == null || parentIndex < 0 || parentIndex === 0.1) {
        if (parentIndex < 0) {
          nodes[i].parentPoint = points[-parentIndex];
        } else if (parentIndex === 0.1) {
          // -0 == 0 无法表示断边 所以用了 0.1 代表 -0
          nodes[i].parentPoint = points[0];
        }
        roots.push(nodes[i]);
      } else {
        nodes[parentIndex].add(nodes[i]);
      }
    }
    return roots;
  }

  constructor(index, point) {
    this.index = index;
    this.point = point;
    this.children = [];
    this.nodeCount = 1;
    this.weight = 0;
  }

  /**
   * 添加子节点
   *
   * @param child      子节点
   * @param edgeWeight 当前节点到子节点的边的 weight
   */
  add(child, edgeWeight) {
    child.parent = this;
    this.children.push(child);
    this.updateCount(child.nodeCount, getRightAngleDistance(this.point, child.point) + child.weight);
  }

  // remove(child, edgeWeight) {
  //     child.parent = null;
  //     children.remove(child);
  //     // updateCount(-(child.nodeCount), -(child.weight + edgeWeight));
  // }

  getChildCount() {
    return this.children.length;
  }

  // 更新插入子树引起的 nodeCount weight 变化
  updateCount(addNodeCount, addWeight) {
    this.nodeCount += addNodeCount;
    this.weight += addWeight;
    if (this.parent != null) {
      this.parent.updateCount(addNodeCount, addWeight);
    }
  }

  preOrderTraversal(callback) {
    callback(this);
    for (let child of this.children) {
      child.preOrderTraversal(callback);
    }
  }

  // 每个点重复一次
  preOrderTraversal1(callback) {
    callback(this);
    for (let child of this.children) {
      child.preOrderTraversal1(callback);
      callback(this);
    }
  }

  getRoot() {
    let root = this;
    while (root.parent != null) {
      root = root.parent;
    }
    return root;
  }
}
