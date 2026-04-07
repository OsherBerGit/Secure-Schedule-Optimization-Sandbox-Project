import React, { useMemo, useState, useEffect } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
  Label
} from 'recharts';
import './FitnessChart.css';

interface FitnessChartProps {
  data: number[];
}

const FitnessChart: React.FC<FitnessChartProps> = ({ data: fitnessHistory = [] }) => {
  const [isDarkMode, setIsDarkMode] = useState(false);

  useEffect(() => {
    const checkDarkMode = () => {
      setIsDarkMode(document.documentElement.classList.contains('dark'));
    };
    checkDarkMode();
    const observer = new MutationObserver(checkDarkMode);
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] });
    return () => observer.disconnect();
  }, []);

  const chartData = useMemo(
    () =>
      fitnessHistory.map((score, index) => ({
        generation: index + 1,
        fitness: score,
      })),
    [fitnessHistory]
  );

  const maxFitness = Math.max(...fitnessHistory);

  if (!fitnessHistory || fitnessHistory.length === 0) return null;

  return (
    <div className="fitness-chart-card">
      <ResponsiveContainer width="100%" height={260}>
        <LineChart
          key={isDarkMode ? 'dark-chart' : 'light-chart'}
          data={chartData}
          margin={{ top: 20, right: 40, left: 10, bottom: 20 }}
        >
          <defs>
            <linearGradient id="fitnessGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3}/>
              <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
            </linearGradient>
          </defs>

          <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />

          <XAxis
            dataKey="generation"
            tick={{ fill: isDarkMode ? '#94a3b8' : '#64748b', fontSize: 12 }}
            tickLine={{ stroke: isDarkMode ? '#334155' : '#e2e8f0' }}
            axisLine={{ stroke: isDarkMode ? '#334155' : '#e2e8f0' }}
            tickFormatter={(value) => `${value}`}
          />
          <YAxis
            domain={['auto', 'auto']}
            tick={{ fill: isDarkMode ? '#94a3b8' : '#64748b', fontSize: 12 }}
            tickLine={{ stroke: isDarkMode ? '#334155' : '#e2e8f0' }}
            axisLine={{ stroke: isDarkMode ? '#334155' : '#e2e8f0' }}
            tickFormatter={(value) => value.toLocaleString()}
          >
            <Label
              value="Score"
              angle={-90}
              position="insideLeft"
              style={{ fill: '#64748b', fontSize: '11px', fontWeight: 800, textTransform: 'uppercase', textAnchor: 'middle' }}
            />
          </YAxis>

          <Tooltip
            contentStyle={{
              background: 'var(--card-bg)',
              border: '1px solid var(--border-color)',
              borderRadius: '8px',
              fontSize: '12px',
              fontWeight: '700',
              boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
              color: 'var(--text-primary)'
            }}
            labelFormatter={(label) => `Generation ${label}`}
            cursor={{ stroke: 'var(--border-color)', strokeWidth: 2 }}
          />

          <ReferenceLine
            y={maxFitness}
            stroke="#10b981"
            strokeDasharray="3 3"
            label={{
              position: 'top',
              value: `Peak: ${maxFitness.toLocaleString()}`,
              fill: '#10b981',
              fontSize: 11,
              fontWeight: 800
            }}
          />

          <Line
            type="monotone"
            dataKey="fitness"
            stroke={isDarkMode ? '#818cf8' : '#4f46e5'}
            strokeWidth={3}
            dot={false}
            activeDot={{ r: 6, fill: isDarkMode ? '#818cf8' : '#4f46e5', strokeWidth: 0 }}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

export default FitnessChart;
