import React, { useMemo } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine, Label } from 'recharts';
import './FitnessChart.css';

interface FitnessChartProps { data: number[]; }

const FitnessChart: React.FC<FitnessChartProps> = ({ data: fitnessHistory = [] }) => {
    const chartData = useMemo(() => fitnessHistory.map((score, index) => ({ generation: index + 1, fitness: score })), [fitnessHistory]);
    const maxFitness = Math.max(...fitnessHistory);

    if (!fitnessHistory || fitnessHistory.length === 0) return null;

    return (
        <div className="fitness-chart-card">
            <ResponsiveContainer width="100%" height={260}>
                <LineChart data={chartData} margin={{ top: 20, right: 40, left: 10, bottom: 20 }}>
                    <defs>
                        <linearGradient id="fitnessGradient" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="var(--primary-color)" stopOpacity={0.3}/>
                            <stop offset="95%" stopColor="var(--primary-color)" stopOpacity={0}/>
                        </linearGradient>
                    </defs>

                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" vertical={false} />

                    <XAxis
                        dataKey="generation"
                        tick={{ fill: 'var(--text-secondary)', fontSize: 12 }}
                        tickLine={{ stroke: 'var(--border-color)' }}
                        axisLine={{ stroke: 'var(--border-color)' }}
                    />
                    <YAxis
                        domain={['auto', 'auto']}
                        tick={{ fill: 'var(--text-secondary)', fontSize: 12 }}
                        tickLine={{ stroke: 'var(--border-color)' }}
                        axisLine={{ stroke: 'var(--border-color)' }}
                    >
                        <Label value="Score" angle={-90} position="insideLeft" style={{ fill: 'var(--text-secondary)', fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', textAnchor: 'middle' }} />
                    </YAxis>

                    <Tooltip
                        contentStyle={{ background: 'var(--card-bg)', border: '1px solid var(--border-color)', borderRadius: '8px', fontSize: '12px', fontWeight: '700', color: 'var(--text-primary)' }}
                        labelFormatter={(label) => `Generation ${label}`}
                        cursor={{ stroke: 'var(--border-color)', strokeWidth: 2 }}
                    />

                    <ReferenceLine
                        y={maxFitness}
                        stroke="#22c55e"
                        strokeDasharray="3 3"
                        label={{ position: 'top', value: `Peak: ${maxFitness.toLocaleString()}`, fill: '#22c55e', fontSize: 11, fontWeight: 800 }}
                    />

                    <Line
                        type="monotone"
                        dataKey="fitness"
                        stroke="var(--primary-color)"
                        strokeWidth={3}
                        dot={false}
                        activeDot={{ r: 6, fill: 'var(--primary-color)', strokeWidth: 0 }}
                        isAnimationActive={false}
                    />
                </LineChart>
            </ResponsiveContainer>
        </div>
    );
}

export default FitnessChart;