import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    ReferenceLine,
} from 'recharts'

interface FitnessChartProps {
    fitnessHistory: number[]
}

interface DataPoint {
    generation: number
    fitness: number
}

const FitnessChart = ({ fitnessHistory }: FitnessChartProps) => {
    const data: DataPoint[] = fitnessHistory.map((score, index) => ({
        generation: index + 1,
        fitness: score,
    }))

    const maxFitness = Math.max(...fitnessHistory)

    return (
        <div style={{
            background: 'white',
            borderRadius: '12px',
            padding: '20px 24px 16px 8px',
            marginTop: '24px',
            marginBottom: '2rem',
            boxShadow: '0 2px 10px rgba(0,0,0,0.07)',
            border: '1px solid #e2e8f0',
        }}>
            <div style={{ paddingLeft: '16px', marginBottom: '16px' }}>
                <h3 style={{
                    margin: 0,
                    color: '#334155',
                    fontSize: '1rem',
                    fontWeight: 600,
                    letterSpacing: '0.02em',
                }}>
                    🧬 Memetic Algorithm - Convergence Graph
                </h3>
                <p style={{
                    margin: '4px 0 0 0',
                    color: '#64748b',
                    fontSize: '0.8rem',
                }}>
                    Best fitness score per generation &nbsp;·&nbsp; {fitnessHistory.length} generations &nbsp;·&nbsp; peak&nbsp;
                    <span style={{ color: '#7c3aed', fontWeight: 600 }}>{maxFitness.toLocaleString()}</span>
                </p>
            </div>

            <ResponsiveContainer width="100%" height={260}>
                <LineChart data={data} margin={{ top: 8, right: 24, left: 0, bottom: 30 }}>
                    <defs>
                        <linearGradient id="fitnessGradient" x1="0" y1="0" x2="1" y2="0">
                            <stop offset="0%"   stopColor="#667eea" />
                            <stop offset="100%" stopColor="#a78bfa" />
                        </linearGradient>
                    </defs>

                    <CartesianGrid
                        strokeDasharray="3 3"
                        stroke="#e2e8f0"
                        vertical={false}
                    />

                    <XAxis
                        dataKey="generation"
                        label={{
                            value: 'Generation',
                            position: 'insideBottom',
                            offset: -2,
                            fill: '#64748b',
                            fontSize: 12,
                        }}
                        tick={{ fill: '#64748b', fontSize: 11 }}
                        axisLine={{ stroke: '#e2e8f0' }}
                        tickLine={false}
                        height={40}
                    />

                    <YAxis
                        label={{
                            value: 'Fitness Score',
                            angle: -90,
                            position: 'insideLeft',
                            offset: 14,
                            fill: '#64748b',
                            fontSize: 12,
                        }}
                        tick={{ fill: '#64748b', fontSize: 11 }}
                        axisLine={false}
                        tickLine={false}
                        width={72}
                        tickFormatter={(v: number) =>
                            v >= 1000 ? `${(v / 1000).toFixed(1)}k` : String(v)
                        }
                    />

                    <Tooltip
                        contentStyle={{
                            background: '#ffffff',
                            border: '1px solid #e2e8f0',
                            borderRadius: '10px',
                            color: '#1e293b',
                            fontSize: '0.85rem',
                            boxShadow: '0 4px 16px rgba(0,0,0,0.1)',
                        }}
                        labelStyle={{ color: '#64748b', marginBottom: '4px' }}
                        labelFormatter={(label) => `Generation ${label}`}
                        formatter={(value) => [
                            typeof value === 'number' ? value.toLocaleString() : String(value),
                            'Fitness',
                        ]}
                        cursor={{ stroke: 'rgba(124,58,237,0.2)', strokeWidth: 1 }}
                    />

                    <ReferenceLine
                        y={maxFitness}
                        stroke="rgba(124,58,237,0.3)"
                        strokeDasharray="4 4"
                        label={{
                            value: `Peak: ${maxFitness.toLocaleString()}`,
                            position: 'insideTopRight',
                            fill: '#7c3aed',
                            fontSize: 11,
                        }}
                    />

                    <Line
                        type="monotone"
                        dataKey="fitness"
                        stroke="url(#fitnessGradient)"
                        strokeWidth={2.5}
                        dot={fitnessHistory.length <= 30
                            ? { r: 3, fill: '#a78bfa', stroke: '#ffffff', strokeWidth: 2 }
                            : false
                        }
                        activeDot={{ r: 6, fill: '#7c3aed', stroke: '#fff', strokeWidth: 2 }}
                        isAnimationActive={true}
                        animationDuration={800}
                        animationEasing="ease-out"
                    />
                </LineChart>
            </ResponsiveContainer>
        </div>
    )
}

export default FitnessChart

