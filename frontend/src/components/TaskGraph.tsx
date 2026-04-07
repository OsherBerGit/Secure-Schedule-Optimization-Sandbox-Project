import { useMemo } from 'react'
import ReactFlow, {
    Background,
    Controls,
    MiniMap,
    Panel,
    MarkerType,
    Position
} from 'reactflow'
import 'reactflow/dist/style.css'
import dagre from 'dagre'
import type { Task, TaskConstraint } from '../types'
import './TaskGraph.css'

interface TaskGraphProps {
    tasks: Task[]
    constraints: TaskConstraint[]
}

const getLayoutedElements = (nodes: any[], edges: any[], direction = 'TB') => {
    const dagreGraph = new dagre.graphlib.Graph()
    dagreGraph.setDefaultEdgeLabel(() => ({}))

    const nodeWidth = 220
    const nodeHeight = 60

    dagreGraph.setGraph({ rankdir: direction })

    nodes.forEach((node) => {
        dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight })
    })

    edges.forEach((edge) => {
        dagreGraph.setEdge(edge.source, edge.target)
    })

    dagre.layout(dagreGraph)

    const layoutedNodes = nodes.map((node) => {
        const nodeWithPosition = dagreGraph.node(node.id)
        return {
            ...node,
            targetPosition: direction === 'TB' ? Position.Top : Position.Left,
            sourcePosition: direction === 'TB' ? Position.Bottom : Position.Right,
            position: {
                x: nodeWithPosition.x - nodeWidth / 2,
                y: nodeWithPosition.y - nodeHeight / 2,
            },
        }
    })

    return { nodes: layoutedNodes, edges }
}

const TaskGraph = ({ tasks, constraints }: TaskGraphProps) => {

    const { nodes, edges } = useMemo(() => {
        const initialNodes = tasks.map(t => ({
            id: t.id.toString(),
            data: {
                label: (
                    <div className="graph-node-content">
                        <span className="graph-node-id">#{t.id}</span>
                        <span className="graph-node-title">{t.title}</span>
                    </div>
                )
            },
            className: 'saas-graph-node',
            position: { x: 0, y: 0 }
        }))

        const initialEdges = constraints.map(c => ({
            id: `e${c.predecessorTaskId}-${c.successorTaskId}`,
            source: c.predecessorTaskId.toString(),
            target: c.successorTaskId.toString(),
            label: c.lagMinutes ? `${c.constraintTypeName} (+${c.lagMinutes}m)` : c.constraintTypeName,
            animated: true,
            style: { stroke: '#6366f1', strokeWidth: 2 },
            labelStyle: { fill: '#475569', fontWeight: 600, fontSize: 11 },
            labelBgStyle: { fill: '#ffffff', fillOpacity: 0.8 },
            markerEnd: {
                type: MarkerType.ArrowClosed,
                color: '#6366f1',
            },
        }))

        return getLayoutedElements(initialNodes, initialEdges, 'TB')
    }, [tasks, constraints])

    if (!tasks.length) {
        return <div className="graph-empty-state">No tasks to display in the graph.</div>
    }

    return (
        <div className="task-graph-wrapper">
            <ReactFlow
                nodes={nodes}
                edges={edges}
                fitView
                className="saas-react-flow"
                nodesDraggable={false}
                nodesConnectable={false}
            >
                <Background color="#cbd5e1" gap={16} />
                <Controls />
                <MiniMap
                    nodeColor="#e2e8f0"
                    maskColor="rgba(248, 250, 252, 0.7)"
                    className="saas-minimap"
                />
                <Panel position="top-right" className="graph-panel">
                    Scroll to zoom | Drag to pan
                </Panel>
            </ReactFlow>
        </div>
    )
}

export default TaskGraph