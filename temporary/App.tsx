import React, { useState, useEffect } from 'react';
import { AspectRatio, Environment, FuelType, Weather, BonfireSettings } from './types';
import { FlameIcon, DownloadIcon, RefreshIcon } from './components/Icons';
import BonfireCanvas from './components/BonfireCanvas';
import AndroidCodeModal from './components/AndroidCodeModal';

function App() {
  const [settings, setSettings] = useState<BonfireSettings>({
    aspectRatio: AspectRatio.Portrait,
    environment: Environment.Forest,
    fuelType: FuelType.Wood,
    weather: Weather.Clear,
    intensity: 1.0,
    temperature: 2500,
    windSpeed: 0,
    playbackSpeed: 1.0,
    timerDuration: 0, 
    timerStartTime: 0
  });

  // Local state for timer inputs
  const [timerValues, setTimerValues] = useState({
      years: 0,
      months: 0,
      days: 0,
      hours: 0,
      minutes: 0,
      seconds: 30
  });
  const [isExtendedTimer, setIsExtendedTimer] = useState(false);
  const [isTimerRunning, setIsTimerRunning] = useState(false);
  const [isCodeModalOpen, setIsCodeModalOpen] = useState(false);

  const getLabel = (key: string): string => {
    const labels: {[key: string]: string} = {
      [AspectRatio.Portrait]: "모바일 (9:16)",
      [AspectRatio.Landscape]: "데스크탑 (16:9)",
      [AspectRatio.Square]: "정방형 (1:1)",
      [Environment.Forest]: "깊은 숲속",
      [Environment.Beach]: "밤바다",
      [Environment.Snowy]: "설원",
      [Environment.SimpleBlack]: "심플 블랙",
      [FuelType.Wood]: "장작 (나무)",
      [FuelType.Paper]: "종이/지푸라기",
      [FuelType.Charcoal]: "숯 (Charcoal)",
      [FuelType.Chemical]: "화학 물질",
      [Weather.Clear]: "맑음",
      [Weather.Rainy]: "비",
      [Weather.Snowy]: "눈",
      [Weather.Windy]: "바람",
    };
    return labels[key] || key;
  };

  const startTimer = () => {
      // Calculate total seconds
      const totalSeconds = 
        (timerValues.years * 31536000) +
        (timerValues.months * 2592000) +
        (timerValues.days * 86400) +
        (timerValues.hours * 3600) +
        (timerValues.minutes * 60) +
        timerValues.seconds;
      
      if (totalSeconds <= 0) {
          // Infinite Mode
          setSettings(s => ({ ...s, timerDuration: 0, timerStartTime: Date.now() }));
          setIsTimerRunning(false);
      } else {
          setSettings(s => ({ ...s, timerDuration: totalSeconds, timerStartTime: Date.now() }));
          setIsTimerRunning(true);
      }
  };

  const resetInfinite = () => {
    setSettings(s => ({ ...s, timerDuration: 0 }));
    setIsTimerRunning(false);
  };

  const handleTimerChange = (field: keyof typeof timerValues, val: string) => {
      setTimerValues(prev => ({
          ...prev,
          [field]: Math.max(0, parseInt(val) || 0)
      }));
  };

  const downloadImage = () => {
    const canvas = document.querySelector('canvas');
    if (canvas) {
        const link = document.createElement('a');
        link.download = `bonfire-${Date.now()}.png`;
        link.href = canvas.toDataURL();
        link.click();
    }
  };

  return (
    <div className="min-h-screen bg-[#0f0f13] text-gray-100 flex flex-col md:flex-row font-sans">
      <AndroidCodeModal isOpen={isCodeModalOpen} onClose={() => setIsCodeModalOpen(false)} />
      
      {/* Left Panel - Controls */}
      <div className="w-full md:w-[450px] flex-shrink-0 flex flex-col border-r border-white/10 glass-panel h-screen overflow-y-auto z-10 relative">
        <div className="p-6 border-b border-white/10 flex items-center gap-3">
          <div className="p-2 bg-gradient-to-br from-orange-500 to-red-600 rounded-lg text-white shadow-lg shadow-orange-500/30">
            <FlameIcon className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold bg-gradient-to-r from-orange-400 to-red-400 bg-clip-text text-transparent">
              모닥불 메이커 Pro
            </h1>
            <p className="text-xs text-gray-400">물리 기반 파티클 엔진</p>
          </div>
        </div>

        <div className="p-6 space-y-8 flex-1">

          {/* Timer Control */}
          <div className="bg-white/5 border border-white/10 rounded-xl p-4">
              <div className="flex items-center justify-between mb-4">
                  <span className="text-sm font-medium text-gray-200">연소 타이머</span>
                  <button 
                    onClick={() => setIsExtendedTimer(!isExtendedTimer)}
                    className="text-[10px] px-2 py-1 rounded border border-white/20 hover:bg-white/5 transition-colors text-gray-400"
                  >
                      {isExtendedTimer ? "간편 모드 (시:분:초)" : "확장 모드 (년:월:일...)"}
                  </button>
              </div>
              
              <div className="grid gap-2 mb-4">
                {/* Y:M:D Row */}
                {isExtendedTimer && (
                    <div className="grid grid-cols-3 gap-2">
                        <div className="bg-black/30 rounded p-2 flex flex-col items-center">
                            <input type="number" className="bg-transparent w-full text-center text-lg font-mono outline-none" value={timerValues.years} onChange={(e) => handleTimerChange('years', e.target.value)} />
                            <span className="text-[10px] text-gray-500">YEAR</span>
                        </div>
                        <div className="bg-black/30 rounded p-2 flex flex-col items-center">
                            <input type="number" className="bg-transparent w-full text-center text-lg font-mono outline-none" value={timerValues.months} onChange={(e) => handleTimerChange('months', e.target.value)} />
                            <span className="text-[10px] text-gray-500">MONTH</span>
                        </div>
                        <div className="bg-black/30 rounded p-2 flex flex-col items-center">
                            <input type="number" className="bg-transparent w-full text-center text-lg font-mono outline-none" value={timerValues.days} onChange={(e) => handleTimerChange('days', e.target.value)} />
                            <span className="text-[10px] text-gray-500">DAY</span>
                        </div>
                    </div>
                )}
                {/* H:M:S Row */}
                <div className="grid grid-cols-3 gap-2">
                    <div className="bg-black/30 rounded p-2 flex flex-col items-center">
                        <input type="number" className="bg-transparent w-full text-center text-lg font-mono outline-none text-orange-200" value={timerValues.hours} onChange={(e) => handleTimerChange('hours', e.target.value)} />
                        <span className="text-[10px] text-gray-500">HOUR</span>
                    </div>
                    <div className="bg-black/30 rounded p-2 flex flex-col items-center">
                        <input type="number" className="bg-transparent w-full text-center text-lg font-mono outline-none text-orange-200" value={timerValues.minutes} onChange={(e) => handleTimerChange('minutes', e.target.value)} />
                        <span className="text-[10px] text-gray-500">MIN</span>
                    </div>
                    <div className="bg-black/30 rounded p-2 flex flex-col items-center">
                        <input type="number" className="bg-transparent w-full text-center text-lg font-mono outline-none text-orange-200" value={timerValues.seconds} onChange={(e) => handleTimerChange('seconds', e.target.value)} />
                        <span className="text-[10px] text-gray-500">SEC</span>
                    </div>
                </div>
              </div>

              <div className="flex gap-2">
                  <button 
                    onClick={startTimer}
                    className="flex-1 py-2 bg-orange-600 hover:bg-orange-700 text-white rounded-lg text-sm font-bold shadow-lg shadow-orange-900/50 transition-all active:scale-95"
                  >
                      {isTimerRunning ? "타이머 재설정 (Restart)" : "불 붙이기 (Ignite)"}
                  </button>
                  {isTimerRunning && (
                    <button 
                        onClick={resetInfinite}
                        className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg text-xs"
                    >
                        무제한
                    </button>
                  )}
              </div>
              <p className="text-[10px] text-gray-500 mt-2 text-center">
                  설정된 시간 동안 불이 타오르며, 연료가 다 되면 서서히 꺼집니다.
              </p>
          </div>

          {/* Aspect Ratio */}
          <div className="space-y-3">
              <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">화면 설정</label>
              <div className="grid grid-cols-3 gap-2">
                {Object.values(AspectRatio).map((ratio) => (
                  <button
                    key={ratio}
                    onClick={() => setSettings({ ...settings, aspectRatio: ratio })}
                    className={`text-xs py-2 px-1 rounded-md border transition-all ${
                      settings.aspectRatio === ratio
                        ? 'bg-orange-500/20 border-orange-500 text-orange-200'
                        : 'bg-white/5 border-white/5 text-gray-400 hover:bg-white/10'
                    }`}
                  >
                    {getLabel(ratio)}
                  </button>
                ))}
              </div>
            </div>

          {/* Environment & Weather */}
          <div className="space-y-3">
             <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">환경 설정</label>
             <div className="grid grid-cols-2 gap-4">
                <div>
                    <label className="text-xs text-gray-400 block mb-2">배경</label>
                    <select
                    value={settings.environment}
                    onChange={(e) => setSettings({ ...settings, environment: e.target.value as Environment })}
                    className="w-full bg-black/40 border border-white/10 rounded-lg px-2 py-2 text-xs text-gray-200 focus:outline-none focus:border-orange-500/50"
                    >
                    {Object.values(Environment).map(env => (
                        <option key={env} value={env}>{getLabel(env)}</option>
                    ))}
                    </select>
                </div>
                <div>
                    <label className="text-xs text-gray-400 block mb-2">날씨</label>
                    <select
                    value={settings.weather}
                    onChange={(e) => setSettings({ ...settings, weather: e.target.value as Weather })}
                    className="w-full bg-black/40 border border-white/10 rounded-lg px-2 py-2 text-xs text-gray-200 focus:outline-none focus:border-orange-500/50"
                    >
                    {Object.values(Weather).map(w => (
                        <option key={w} value={w}>{getLabel(w)}</option>
                    ))}
                    </select>
                </div>
             </div>
          </div>

          {/* Fuel Type */}
           <div className="space-y-3">
                <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">연료 (재질)</label>
                <div className="grid grid-cols-2 gap-2">
                    {Object.values(FuelType).map(f => (
                        <button
                            key={f}
                            onClick={() => setSettings({...settings, fuelType: f})}
                            className={`px-3 py-2 rounded-lg text-xs border flex items-center justify-between transition-all ${
                                settings.fuelType === f 
                                ? 'bg-white/10 border-white/30 text-white' 
                                : 'bg-transparent border-white/5 text-gray-400 hover:bg-white/5'
                            }`}
                        >
                            {getLabel(f)}
                        </button>
                    ))}
                </div>
            </div>

            {/* Sliders */}
            <div className="space-y-6 pt-2">
                
                {/* Temperature */}
                <div className="space-y-2">
                    <div className="flex justify-between text-xs text-gray-400">
                        <span>연소 온도 (색상 변화)</span>
                        <span style={{ color: settings.temperature < 3000 ? '#ff4500' : settings.temperature < 6000 ? '#ffcc00' : '#88ccff' }}>
                            {settings.temperature}K
                        </span>
                    </div>
                    <input 
                        type="range" 
                        min="1000" 
                        max="8000" 
                        step="100" 
                        value={settings.temperature}
                        onChange={(e) => setSettings({...settings, temperature: parseFloat(e.target.value)})}
                        className="w-full h-1 bg-gradient-to-r from-red-600 via-orange-400 via-yellow-200 to-blue-300 rounded-lg appearance-none cursor-pointer"
                    />
                </div>

                {/* Intensity */}
                <div className="space-y-2">
                    <div className="flex justify-between text-xs text-gray-400">
                        <span>불꽃 크기</span>
                        <span>{Math.round(settings.intensity * 100)}%</span>
                    </div>
                    <input 
                        type="range" 
                        min="0.3" 
                        max="2.0" 
                        step="0.1" 
                        value={settings.intensity}
                        onChange={(e) => setSettings({...settings, intensity: parseFloat(e.target.value)})}
                        className="w-full h-1 bg-gray-700 rounded-lg appearance-none cursor-pointer accent-orange-500"
                    />
                </div>

                {/* Playback Speed */}
                <div className="space-y-2">
                    <div className="flex justify-between text-xs text-gray-400">
                        <span>재생 속도 (시간 조절)</span>
                        <span>{settings.playbackSpeed}x</span>
                    </div>
                    <div className="flex items-center gap-2">
                         <span className="text-[10px] text-gray-600">Slow</span>
                         <input 
                            type="range" 
                            min="0.1" 
                            max="3.0" 
                            step="0.1" 
                            value={settings.playbackSpeed}
                            onChange={(e) => setSettings({...settings, playbackSpeed: parseFloat(e.target.value)})}
                            className="w-full h-1 bg-gray-700 rounded-lg appearance-none cursor-pointer accent-green-500"
                        />
                        <span className="text-[10px] text-gray-600">Fast</span>
                    </div>
                </div>

                {/* Wind */}
                <div className="space-y-2">
                    <div className="flex justify-between text-xs text-gray-400">
                        <span>바람</span>
                        <span>{settings.windSpeed}</span>
                    </div>
                    <input 
                        type="range" 
                        min="-3" 
                        max="3" 
                        step="0.5" 
                        value={settings.windSpeed}
                        onChange={(e) => setSettings({...settings, windSpeed: parseFloat(e.target.value)})}
                        className="w-full h-1 bg-gray-700 rounded-lg appearance-none cursor-pointer accent-blue-500"
                    />
                </div>
            </div>

           <div className="pt-4 border-t border-white/10 space-y-2">
                <button
                    onClick={downloadImage}
                    className="w-full py-3 bg-white/10 hover:bg-white/20 border border-white/10 text-white rounded-xl font-medium flex items-center justify-center gap-2 transition-colors"
                >
                    <DownloadIcon className="w-5 h-5" />
                    현재 화면 저장 (PNG)
                </button>
                <button
                    onClick={() => setIsCodeModalOpen(true)}
                    className="w-full py-3 bg-green-900/40 hover:bg-green-800/50 border border-green-500/20 text-green-200 rounded-xl font-medium flex items-center justify-center gap-2 transition-colors"
                >
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 12c-2.9 0-5.2-2.3-5.2-5.2 0-4.8 6.3-8.8 6.3-8.8s6.3 4 6.3 8.8c0 2.9-2.3 5.2-5.2 5.2"/><path d="M12 14v8"/><path d="M16 18h-8"/></svg>
                    안드로이드 소스 (Kotlin)
                </button>
           </div>
        </div>
      </div>

      {/* Right Panel - Preview */}
      <div className="flex-1 bg-black flex flex-col items-center justify-center p-6 md:p-12 relative overflow-hidden">
        
        {/* Content Area */}
        <div className="relative w-full h-full flex items-center justify-center">
             <div className={`relative shadow-2xl rounded-2xl overflow-hidden ring-1 ring-white/10 bg-black w-full shadow-orange-900/20`} 
                style={{
                    aspectRatio: settings.aspectRatio === AspectRatio.Landscape ? '16/9' : 
                                 settings.aspectRatio === AspectRatio.Square ? '1/1' : '9/16',
                    maxWidth: settings.aspectRatio === AspectRatio.Landscape ? '1000px' : 
                              settings.aspectRatio === AspectRatio.Square ? '600px' : '500px',
                    maxHeight: '85vh'
                }}
             >
                <BonfireCanvas settings={settings} />
                
                {/* Overlay Indicators */}
                <div className="absolute top-4 right-4 flex flex-col items-end gap-2 pointer-events-none">
                    {isTimerRunning && (
                        <div className="px-2 py-1 bg-red-500/20 border border-red-500/50 rounded text-[10px] text-red-200 animate-pulse">
                            BURNING
                        </div>
                    )}
                    {settings.playbackSpeed !== 1.0 && (
                        <div className="px-2 py-1 bg-white/10 border border-white/20 rounded text-[10px] text-gray-300">
                            SPEED {settings.playbackSpeed}x
                        </div>
                    )}
                </div>
             </div>
        </div>
      </div>
    </div>
  );
}

export default App;