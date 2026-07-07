import { StructureInfo, StructureType } from '../renderers/structure-renderer.types';

export const STRUCTURE_INFO: Record<StructureType, StructureInfo> = {
  array: {
    title: '数组 Array',
    definition: '数组是一组连续存储的元素，支持通过下标进行随机访问。',
    features: ['连续内存', '随机访问快', '插入和删除可能需要移动后续元素'],
    operations: [
      { name: '访问', complexity: 'O(1)', description: '通过下标直接定位元素。' },
      { name: '查找', complexity: 'O(n)', description: '从左到右扫描，直到找到目标。' },
      { name: '插入', complexity: 'O(n)', description: '在指定位置插入元素，后续元素右移。' },
      { name: '删除', complexity: 'O(n)', description: '删除指定位置元素，后续元素左移。' },
      { name: '交换', complexity: 'O(1)', description: '交换两个下标对应的元素。' },
    ],
    useCases: ['顺序表', '缓存', '矩阵', '动态数组底层结构'],
  },
  stack: {
    title: '栈 Stack',
    definition: '栈是一种后进先出 LIFO 的线性数据结构，只允许在栈顶操作。',
    features: ['只在栈顶操作', '后进先出', '入栈和出栈通常为常数时间'],
    operations: [
      { name: '入栈', complexity: 'O(1)', description: '向栈顶压入一个元素。' },
      { name: '出栈', complexity: 'O(1)', description: '弹出并移除栈顶元素。' },
      { name: '查看栈顶', complexity: 'O(1)', description: '查看栈顶元素但不移除。' },
    ],
    useCases: ['函数调用栈', '括号匹配', '表达式求值', '撤销操作'],
  },
  queue: {
    title: '队列 Queue',
    definition: '队列是一种先进先出 FIFO 的线性数据结构，队尾入队，队头出队。',
    features: ['队尾入队', '队头出队', '先进先出'],
    operations: [
      { name: '入队', complexity: 'O(1)', description: '向队尾加入元素。' },
      { name: '出队', complexity: 'O(1)', description: '从队头移除元素。' },
      { name: '查看队头', complexity: 'O(1)', description: '查看队头元素但不移除。' },
    ],
    useCases: ['任务调度', '消息队列', 'BFS', '缓冲区'],
  },
  'linked-list': {
    title: '链表 Linked List',
    definition: '链表由节点组成，每个节点保存数据和指向下一个节点的引用。',
    features: ['非连续存储', '插入删除灵活', '随机访问较慢'],
    operations: [
      { name: '查找', complexity: 'O(n)', description: '从头节点开始逐个比较。' },
      { name: '头插', complexity: 'O(1)', description: '在头部插入新节点。' },
      { name: '删除', complexity: 'O(n)', description: '先定位目标节点，再调整指针。' },
    ],
    useCases: ['链式栈', '链式队列', '邻接表', '内存管理'],
  },
  'binary-tree': {
    title: '二叉树 Binary Tree',
    definition: '二叉树是每个节点最多有两个子节点的层级结构。这里按二叉搜索树规则演示查找和插入。',
    features: ['层级结构', '适合递归描述', '平衡时查找效率较高'],
    operations: [
      { name: '查找', complexity: 'O(log n) / O(n)', description: '按大小关系沿左/右子树向下搜索。' },
      { name: '插入', complexity: 'O(log n) / O(n)', description: '找到空子树位置后插入新节点。' },
      { name: '遍历', complexity: 'O(n)', description: '选择前序、中序、后序或层序访问全部节点。' },
    ],
    useCases: ['搜索树', '堆', '语法树', '决策树'],
  },
  'b-plus-tree': {
    title: 'B+ 树 B+ Tree',
    definition: 'B+ 树是一种多路平衡搜索树，常用于数据库和文件系统索引。',
    features: ['多路平衡', '内部节点保存索引', '数据集中在叶子节点', '叶子节点链表便于范围查询'],
    operations: [
      { name: '查找', complexity: 'O(log n)', description: '从根节点沿索引向下定位叶子节点。' },
      { name: '插入', complexity: 'O(log n)', description: '插入叶子节点，必要时分裂并上提索引。' },
      { name: '删除', complexity: 'O(log n)', description: '删除后可能触发借位或合并。' },
      { name: '范围查找', complexity: 'O(log n + k)', description: '定位起点后沿叶子链表顺序扫描。' },
    ],
    useCases: ['数据库索引', '文件系统索引', '范围查询', '磁盘块管理'],
  },
};
