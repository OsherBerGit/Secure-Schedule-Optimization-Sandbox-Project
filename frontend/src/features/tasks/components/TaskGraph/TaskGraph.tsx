import { useMemo } from "react";
import ReactFlow, { Background, Controls, MiniMap, Panel, MarkerType, Position, type Node, type Edge } from "reactflow";
import "reactflow/dist/style.css";
import dagre from "dagre";
import type { Task, TaskConstraint } from "../../../../types";
import "./TaskGraph.css";

interface TaskGraphProps {
    tasks: Task[];
    constraints: TaskConstraint[];
}

const getLayoutedElements = (nodes: Node[], edges: Edge[], direction = "TB") => {
    const dagreGraph = new dagre.graphlib.Graph();
    dagreGraph.setDefaultEdgeLabel(() => ({}));

    const nodeWidth = 220;
    const nodeHeight = 60;

    dagreGraph.setGraph({ rankdir: direction });

    nodes.forEach(node => {
        dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight });
    });

    edges.forEach(edge => {
        dagreGraph.setEdge(edge.source, edge.target);
    });

    dagre.layout(dagreGraph);

    const layoutedNodes = nodes.map(node => {
        const nodeWithPosition = dagreGraph.node(node.id);
        return {
            ...node,
            targetPosition: direction === "TB" ? Position.Top : Position.Left,
            sourcePosition: direction === "TB" ? Position.Bottom : Position.Right,
            position: {
                x: nodeWithPosition.x - nodeWidth / 2,
                y: nodeWithPosition.y - nodeHeight / 2
            }
        };
    });

    return { nodes: layoutedNodes, edges };
};

const TaskGraph = ({ tasks, constraints }: TaskGraphProps) => {
    const { nodes, edges } = useMemo(() => {
        const initialNodes: Node[] = tasks.map(task => ({
            id: task.id.toString(),
            data: {
                label: (
                    <div className="graph-node-content">
                        <span className="graph-node-id">#{task.id}</span>
                        <span className="graph-node-title">{task.title}</span>
                    </div>
                )
            },
            className: "saas-graph-node",
            position: { x: 0, y: 0 }
        }));

        const initialEdges: Edge[] = constraints.map(constraint => ({
            id: `e${constraint.predecessorTaskId}-${constraint.successorTaskId}`,
            source: constraint.predecessorTaskId.toString(),
            target: constraint.successorTaskId.toString(),
            label: constraint.lagMinutes ? `${constraint.constraintTypeName} (+${constraint.lagMinutes}m)` : constraint.constraintTypeName,
            animated: true,
            style: { stroke: "var(--primary-color)", strokeWidth: 2 },
            labelStyle: {
                fill: "var(--text-secondary)",
                fontWeight: 600,
                fontSize: 11
            },
            labelBgStyle: { fill: "var(--card-bg)", fillOpacity: 0.9 },
            markerEnd: {
                type: MarkerType.ArrowClosed,
                color: "var(--primary-color)"
            }
        }));

        return getLayoutedElements(initialNodes, initialEdges, "TB");
    }, [tasks, constraints]);

    if (!tasks.length) {
        return <div className="graph-empty-state">No tasks to display in the graph.</div>;
    }

    return (
        <div className="task-graph-wrapper">
            <ReactFlow nodes={nodes} edges={edges} fitView className="saas-react-flow" nodesDraggable={false} nodesConnectable={false}>
                <Background color="var(--border-color)" gap={16} />
                <Controls />
                <MiniMap nodeColor="var(--border-color)" maskColor="var(--bg-main)" className="saas-minimap graph-minimap-translucent" />
                <Panel position="top-right" className="graph-panel">
                    Scroll to zoom | Drag to pan
                </Panel>
            </ReactFlow>
        </div>
    );
};

export default TaskGraph;
