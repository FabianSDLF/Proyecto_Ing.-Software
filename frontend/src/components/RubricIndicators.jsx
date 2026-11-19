export default function RubricIndicators({ indicators, points, readOnly = false, onChange }) {
  const groups = Object.groupBy(indicators, i => i.dimension || 'General');
  return <div className="space-y-4">
    {Object.entries(groups).map(([dimension, items]) => <fieldset key={dimension} className="min-w-0 border rounded-xl p-3 space-y-3">
      <legend className="px-2 font-bold text-blue-900">Dimensión {dimension}</legend>
      {items.map(ind => {
        const code = ind.criterionCode || ind.id;
        const value = readOnly ? (ind.points ?? points[code] ?? '') : (points[code] ?? '');
        return <div key={code} className="border-b last:border-0 pb-3 flex flex-col sm:flex-row gap-3 sm:items-center">
          <label htmlFor={`indicator-${code}`} className="flex-1 text-sm leading-relaxed"><strong>{code}</strong> {ind.text}</label>
          {readOnly ? <output id={`indicator-${code}`} className="font-bold">{value === '' ? 'Sin observar' : `${value} / 7 pts`}</output> :
            <select id={`indicator-${code}`} aria-label={`Puntaje indicador ${code}`} value={value}
              onChange={e => onChange(code, e.target.value)} className="min-h-11 border rounded-lg p-2 bg-white sm:w-36">
              <option value="">Sin observar</option>
              {[2,3,4,5,6,7].map(n => <option key={n} value={n}>{n} puntos</option>)}
            </select>}
        </div>;
      })}
    </fieldset>)}
  </div>;
}
