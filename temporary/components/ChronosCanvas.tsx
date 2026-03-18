import React, { useRef, useEffect } from 'react';
import { ChronosSettings, Environment, FuelType, Weather } from '../types';

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  size: number;
  baseColor: { r: number, g: number, b: number };
  type: 'fire' | 'smoke' | 'spark' | 'ash' | 'rain' | 'snow';
  targetY?: number; // Target Y position for rain/snow impact
}

interface Ripple {
  x: number;
  y: number;
  radius: number;
  maxRadius: number;
  alpha: number;
  life: number;
}

interface GrassBlade {
  x: number;
  y: number; // Position on screen (higher y is closer to viewer)
  height: number;
  lean: number;
  color: string;
  swaySpeed: number;
  swayOffset: number;
}

export default function ChronosCanvas({ settings }: { settings: ChronosSettings }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const requestRef = useRef<number>(0);
  const particles = useRef<Particle[]>([]);
  const stars = useRef<{x: number, y: number, alpha: number}[]>([]);
  const ripples = useRef<Ripple[]>([]);
  const grassBlades = useRef<GrassBlade[]>([]);
  
  // Track lifecycle state for logging/visuals if needed
  const lifecycleStateRef = useRef<number>(1); 

  // Initialize static elements (Stars, Grass) on mount or resize
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    
    // Stars
    stars.current = [];
    for(let i=0; i<150; i++) {
        stars.current.push({
            x: Math.random() * canvas.width,
            y: Math.random() * canvas.height * 0.6,
            alpha: Math.random()
        });
    }

    // Grass - Distribute randomly for 3D effect
    grassBlades.current = [];
    const bladeCount = 400; // More grass for density
    const horizonY = canvas.height * 0.75; // Grass starts here
    
    for(let i=0; i<bladeCount; i++) {
        const y = horizonY + Math.random() * (canvas.height - horizonY);
        // Perspective: Lower on screen (higher y) = closer = larger
        const depthFactor = (y - horizonY) / (canvas.height - horizonY); 
        
        // Color variation based on depth
        const r = 20 + Math.random() * 20;
        const g = 50 + depthFactor * 40 + Math.random() * 20; 
        const b = 20 + Math.random() * 20;
        
        grassBlades.current.push({
            x: Math.random() * canvas.width,
            y: y,
            height: (30 + Math.random() * 40) * (0.5 + depthFactor),
            lean: (Math.random() - 0.5) * 10,
            color: `rgb(${Math.floor(r)}, ${Math.floor(g)}, ${Math.floor(b)})`,
            swaySpeed: 0.002 + Math.random() * 0.003,
            swayOffset: Math.random() * Math.PI * 2
        });
    }
    // Sort by Y so closer grass draws on top of further grass
    grassBlades.current.sort((a, b) => a.y - b.y);

  }, [settings.aspectRatio, settings.environment]); // Re-init on env change too

  const getTemperatureColor = (temp: number): { r: number, g: number, b: number } => {
    if (temp < 2000) return { r: 255, g: 60, b: 0 };   // Deep Red
    if (temp < 3500) return { r: 255, g: 140, b: 0 };  // Orange
    if (temp < 5000) return { r: 255, g: 200, b: 50 }; // Yellow
    if (temp < 7500) return { r: 255, g: 255, b: 255 };// White
    return { r: 100, g: 200, b: 255 };                 // Blue/Plasma
  };

  const spawnParticle = (w: number, h: number, intensityMult: number) => {
    const centerY = h * 0.85;
    const centerX = w / 2;
    
    // Apply lifecycle intensity
    const currentIntensity = settings.intensity * intensityMult;
    lifecycleStateRef.current = intensityMult;

    if (currentIntensity <= 0.01) return; // Fire is out

    // Fuel Characteristics
    let fuelSpeedMod = 1.0;
    let fuelLifeMod = 1.0;
    let fuelSpreadMod = 1.0;
    let hasAsh = false;
    let baseColor = getTemperatureColor(settings.temperature);

    if (settings.fuelType === FuelType.Paper) {
        fuelSpeedMod = 1.8;
        fuelLifeMod = 0.6;
        hasAsh = true;
    } else if (settings.fuelType === FuelType.Charcoal) {
        fuelSpeedMod = 0.4;
        fuelLifeMod = 1.5;
        fuelSpreadMod = 1.5;
        if (settings.temperature < 4000) baseColor = { r: 255, g: 40, b: 10 };
    } else if (settings.fuelType === FuelType.Chemical) {
        baseColor = { r: 50, g: 255, b: 100 }; 
        if (Math.random() > 0.5) baseColor = { r: 180, g: 50, b: 255 };
    }

    // Spawn Count based on intensity
    const count = Math.floor(3 * currentIntensity);
    
    for(let i=0; i<count; i++) {
        const angle = (Math.random() - 0.5) * Math.PI * 0.5; 
        const speed = (2 + Math.random() * 3) * fuelSpeedMod * (settings.temperature / 3000); 
        
        particles.current.push({
            x: centerX + (Math.random() - 0.5) * 60 * currentIntensity * fuelSpreadMod,
            y: centerY + (Math.random() - 0.5) * 20,
            vx: Math.sin(angle) * 0.5 + (settings.windSpeed * 0.2),
            vy: -speed,
            life: 1.0,
            maxLife: 1.0 * fuelLifeMod,
            size: (15 + Math.random() * 20) * currentIntensity,
            baseColor: baseColor,
            type: 'fire'
        });
    }

    // Smoke
    let smokeChance = 0.2;
    if (intensityMult < 0.5) smokeChance = 0.8; 

    if (Math.random() < smokeChance * currentIntensity) {
        particles.current.push({
            x: centerX + (Math.random() - 0.5) * 50,
            y: centerY - 40,
            vx: (Math.random() - 0.5) + (settings.windSpeed * 0.5),
            vy: -1 - Math.random(),
            life: 1.0,
            maxLife: 1.0,
            size: 20 + Math.random() * 20,
            baseColor: {r:100, g:100, b:100},
            type: 'smoke'
        });
    }

    // Ash 
    if (hasAsh || (settings.fuelType === FuelType.Wood && settings.temperature > 4000)) {
         if (Math.random() < 0.2 * currentIntensity) {
             particles.current.push({
                 x: centerX + (Math.random() - 0.5) * 40,
                 y: centerY - 20,
                 vx: (Math.random() - 0.5) * 3 + settings.windSpeed,
                 vy: -3 - Math.random() * 4,
                 life: 1.0,
                 maxLife: 1.0,
                 size: 2 + Math.random() * 2,
                 baseColor: {r:200, g:200, b:200},
                 type: 'ash'
             });
         }
    }

    // Sparks 
    let sparkChance = 0.1;
    if (settings.temperature > 5000) sparkChance = 0.3;
    
    if (Math.random() < sparkChance * currentIntensity) {
        particles.current.push({
            x: centerX + (Math.random() - 0.5) * 40,
            y: centerY,
            vx: (Math.random() - 0.5) * 2 + (settings.windSpeed * 0.3),
            vy: -3 - Math.random() * 3,
            life: 1.0,
            maxLife: 1.0,
            size: 2 + Math.random(),
            baseColor: {r: 255, g: 220, b: 100},
            type: 'spark'
        });
    }

    // Weather particles
    if (settings.weather === Weather.Rainy || settings.weather === Weather.Snowy) {
        const precipCount = settings.weather === Weather.Rainy ? 5 : 2;
        
        // Define Water Area (Horizon)
        const waterHorizonY = settings.environment === Environment.Beach ? h * 0.6 : h;
        
        for(let i=0; i<precipCount; i++) {
             // For Beach: Target Y is somewhere on the water surface (horizon to bottom)
             // For others: Target Y is bottom of screen
             let targetY = h + 10;
             if (settings.environment === Environment.Beach) {
                 targetY = waterHorizonY + Math.random() * (h - waterHorizonY);
             }

             particles.current.push({
                x: Math.random() * w,
                y: -10,
                vx: settings.windSpeed + (Math.random() - 0.5),
                vy: settings.weather === Weather.Rainy ? 25 + Math.random() * 5 : 4 + Math.random() * 2,
                life: 100.0, // Long life to reach target
                maxLife: 100.0,
                size: settings.weather === Weather.Rainy ? 20 : 5,
                baseColor: {r:255, g:255, b:255},
                type: settings.weather === Weather.Rainy ? 'rain' : 'snow',
                targetY: targetY
            });
        }
    }
  };

  const draw = (timestamp: number) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const w = canvas.width;
    const h = canvas.height;
    
    // --- Time Calculation ---
    const now = Date.now();
    let intensityMult = 1.0;
    if (settings.timerDuration > 0) {
        const elapsed = (now - settings.timerStartTime) / 1000;
        const duration = settings.timerDuration;
        const progress = elapsed / duration;

        if (progress >= 1) {
            intensityMult = 0;
        } else {
            const ignitionTime = Math.min(3, duration * 0.2);
            if (elapsed < ignitionTime) {
                intensityMult = elapsed / ignitionTime;
            } else {
                intensityMult = 1.0 - Math.pow(progress, 2);
                if (intensityMult < 0) intensityMult = 0;
            }
        }
    }

    // --- DRAW BACKGROUND ---
    let bgTop = '#0f0f13';
    let bgBottom = '#1a1a20';

    if (settings.environment === Environment.Forest) { bgTop = '#051015'; bgBottom = '#0a1a0f'; }
    if (settings.environment === Environment.Beach) { bgTop = '#020510'; bgBottom = '#0a0f20'; } // Darker sky for contrast
    if (settings.environment === Environment.Snowy) { bgTop = '#0a0a10'; bgBottom = '#e0e0ec'; }
    if (settings.environment === Environment.SimpleBlack) { bgTop = '#000000'; bgBottom = '#000000'; }

    const grad = ctx.createLinearGradient(0, 0, 0, h);
    grad.addColorStop(0, bgTop);
    grad.addColorStop(1, bgBottom);
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, w, h);

    // Stars
    if (settings.weather === Weather.Clear || settings.weather === Weather.Snowy) {
        ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
        stars.current.forEach(star => {
            if (Math.random() > 0.99) star.alpha = Math.random();
            ctx.globalAlpha = star.alpha;
            ctx.beginPath();
            ctx.arc(star.x, star.y, 1.5, 0, Math.PI * 2);
            ctx.fill();
        });
        ctx.globalAlpha = 1.0;
    }
    
    const dt = settings.playbackSpeed; 
    const centerY = h * 0.85;

    // --- ENVIRONMENT SPECIFIC EXTRAS ---

    // Forest Grass (3D Distributed)
    if (settings.environment === Environment.Forest) {
        const wind = settings.windSpeed;
        
        grassBlades.current.forEach(blade => {
            const sway = Math.sin(timestamp * blade.swaySpeed * dt + blade.swayOffset) * (blade.height * 0.2 + wind * 5);
            
            ctx.strokeStyle = blade.color;
            ctx.lineWidth = 1 + (blade.y / h) * 1.5; // Thicker if closer
            ctx.lineCap = 'round';

            ctx.beginPath();
            ctx.moveTo(blade.x, blade.y);
            // Control point for curve
            const cpX = blade.x + blade.lean + sway;
            const cpY = blade.y - blade.height * 0.5;
            // End point
            const endX = blade.x + blade.lean * 2 + sway * 1.5;
            const endY = blade.y - blade.height;

            ctx.quadraticCurveTo(cpX, cpY, endX, endY);
            ctx.stroke();
        });
    }

    // Beach Water Surface & Ripples (Wide Water)
    if (settings.environment === Environment.Beach) {
        const horizonY = h * 0.6; // Higher horizon for wide water
        
        // Draw water gradient
        const waterGrad = ctx.createLinearGradient(0, horizonY, 0, h);
        waterGrad.addColorStop(0, '#051025'); // Dark horizon
        waterGrad.addColorStop(1, '#0a1a30'); // Slightly lighter closer
        ctx.fillStyle = waterGrad;
        ctx.fillRect(0, horizonY, w, h - horizonY);
        
        // Fire Reflection on water
        // An oval shape stretched horizontally under the fire
        const reflectionY = centerY + 20;
        const reflectionWidth = 100 * intensityMult;
        const reflectionHeight = 30 * intensityMult;
        
        if (reflectionWidth > 1) {
            const reflectGrad = ctx.createRadialGradient(w/2, reflectionY, 5, w/2, reflectionY, reflectionWidth);
            reflectGrad.addColorStop(0, 'rgba(255, 100, 50, 0.4)');
            reflectGrad.addColorStop(0.5, 'rgba(255, 150, 50, 0.1)');
            reflectGrad.addColorStop(1, 'rgba(0,0,0,0)');
            
            ctx.fillStyle = reflectGrad;
            ctx.beginPath();
            ctx.ellipse(w/2, reflectionY, reflectionWidth, reflectionHeight, 0, 0, Math.PI * 2);
            ctx.fill();

            // Reflection flickering lines
            ctx.strokeStyle = `rgba(255, 150, 50, ${0.1 + Math.random() * 0.1})`;
            ctx.lineWidth = 2;
            for(let i=0; i<3; i++) {
                const lw = reflectionWidth * (0.8 + Math.random() * 0.4);
                const ly = reflectionY + (Math.random() - 0.5) * 10;
                ctx.beginPath();
                ctx.moveTo(w/2 - lw/2, ly);
                ctx.lineTo(w/2 + lw/2, ly);
                ctx.stroke();
            }
        }

        // Update & Draw Ripples
        ctx.strokeStyle = 'rgba(200, 220, 255, 0.2)';
        ctx.lineWidth = 1;
        
        ripples.current.forEach((r, index) => {
            r.life -= 0.02 * dt;
            r.radius += 0.5 * dt;
            r.alpha = r.life;
            
            if (r.life <= 0) {
                ripples.current.splice(index, 1);
            } else {
                ctx.globalAlpha = r.alpha;
                // Perspective skew: Ripples near horizon (lower y relative to horizon) are flatter
                // Normalize Y between horizon and bottom: 0 (horizon) to 1 (bottom)
                const depth = Math.max(0.1, (r.y - horizonY) / (h - horizonY));
                const skewY = 0.1 + depth * 0.3; // 0.1 (flat) to 0.4 (round)

                ctx.beginPath();
                ctx.ellipse(r.x, r.y, r.radius, r.radius * skewY, 0, 0, Math.PI * 2);
                ctx.stroke();
            }
        });
        ctx.globalAlpha = 1.0;
    }


    // Logs
    const centerX = w / 2;
    
    let burnProgress = 0;
    if (settings.timerDuration > 0) {
        burnProgress = ((now - settings.timerStartTime) / 1000) / settings.timerDuration;
        if (burnProgress > 1) burnProgress = 1;
    }
    
    const r = 62 - (42 * burnProgress);
    const g = 39 - (19 * burnProgress);
    const b = 35 - (15 * burnProgress);
    const logColor = `rgb(${Math.floor(r)}, ${Math.floor(g)}, ${Math.floor(b)})`;

    ctx.fillStyle = logColor;
    ctx.strokeStyle = '#111';
    ctx.lineWidth = 2;

    const drawLog = (x: number, y: number, angle: number) => {
        ctx.save();
        ctx.translate(x, y);
        ctx.rotate(angle);
        ctx.beginPath();
        ctx.roundRect(-40, -10, 80, 20, 5);
        ctx.fill();
        ctx.stroke();
        ctx.restore();
    };

    // Draw Logs (always roughly at centerY)
    if (settings.fuelType !== FuelType.Paper) {
        drawLog(centerX, centerY + 10, 0);
        drawLog(centerX - 20, centerY - 5, -Math.PI / 6);
        drawLog(centerX + 20, centerY - 5, Math.PI / 6);
    } else {
        ctx.fillStyle = burnProgress > 0.8 ? '#333' : '#e0e0e0';
        drawLog(centerX, centerY + 10, 0);
        drawLog(centerX, centerY, Math.PI / 12);
    }


    // --- UPDATE & DRAW PARTICLES ---
    spawnParticle(w, h, intensityMult);

    particles.current = particles.current.filter(p => p.life > 0 && p.y < h + 50 && p.x > -50 && p.x < w + 50);

    ctx.globalCompositeOperation = 'lighter';

    particles.current.forEach(p => {
        p.life -= 0.015 * dt;
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        p.size *= (p.type === 'smoke' ? 1.005 : 0.97);

        if (p.type === 'fire') {
            const lifeRatio = p.life / p.maxLife;
            let r = p.baseColor.r;
            let g = p.baseColor.g;
            let b = p.baseColor.b;

            if (lifeRatio < 0.5) {
                g *= lifeRatio * 2;
                b *= lifeRatio * 2;
            }
            ctx.fillStyle = `rgba(${Math.floor(r)}, ${Math.floor(g)}, ${Math.floor(b)}, ${p.life})`;
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.fill();
        } 
        else if (p.type === 'spark' || p.type === 'ash') {
            const alpha = p.type === 'ash' ? p.life * 0.5 : p.life;
            ctx.fillStyle = `rgba(${p.baseColor.r}, ${p.baseColor.g}, ${p.baseColor.b}, ${alpha})`;
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.fill();
        }
    });

    ctx.globalCompositeOperation = 'source-over';

    particles.current.forEach(p => {
        // Rain/Snow collision
        if ((p.type === 'rain' || p.type === 'snow') && settings.environment === Environment.Beach) {
             // Check if hit target Y (water surface point)
             if (p.targetY && p.y >= p.targetY) {
                 if (Math.random() > 0.3) { 
                     ripples.current.push({
                         x: p.x,
                         y: p.targetY,
                         radius: 1,
                         maxRadius: 15 + Math.random() * 10,
                         alpha: 1.0,
                         life: 1.0
                     });
                 }
                 p.life = 0; // Disappear
             }
        }

        if (p.type === 'smoke') {
            ctx.fillStyle = `rgba(100, 100, 100, ${p.life * 0.2})`;
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.fill();
        }
        else if (p.type === 'rain') {
            ctx.strokeStyle = `rgba(200, 200, 255, 0.6)`;
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(p.x + p.vx * 2 * dt, p.y + p.size);
            ctx.stroke();
        }
        else if (p.type === 'snow') {
             p.x += Math.sin(timestamp / 1000 + p.life * 10) * 0.5 * dt;
             ctx.fillStyle = `rgba(255, 255, 255, 0.8)`;
             ctx.beginPath();
             ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
             ctx.fill();
        }
    });

    // GLOW
    const baseGlow = getTemperatureColor(settings.temperature);
    const glowAlpha = 0.2 * intensityMult;
    
    if (glowAlpha > 0.01) {
        // Adjust glow position based on logs
        const glowGrad = ctx.createRadialGradient(centerX, centerY, 20, centerX, centerY, 150 + Math.random() * 10);
        glowGrad.addColorStop(0, `rgba(${baseGlow.r}, ${baseGlow.g}, ${baseGlow.b}, ${glowAlpha})`);
        glowGrad.addColorStop(1, `rgba(${baseGlow.r}, ${baseGlow.g}, ${baseGlow.b}, 0)`);
        ctx.fillStyle = glowGrad;
        ctx.globalCompositeOperation = 'lighter';
        ctx.fillRect(0, 0, w, h);
        ctx.globalCompositeOperation = 'source-over';
    }

    requestRef.current = requestAnimationFrame(draw);
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const updateSize = () => {
        const container = canvas.parentElement;
        if (!container) return;
        canvas.width = container.clientWidth;
        canvas.height = container.clientHeight;
        stars.current = [];
        for(let i=0; i<150; i++) {
            stars.current.push({
                x: Math.random() * canvas.width,
                y: Math.random() * canvas.height * 0.6,
                alpha: Math.random()
            });
        }
    };
    updateSize();
    window.addEventListener('resize', updateSize);
    requestRef.current = requestAnimationFrame(draw);

    return () => {
        window.removeEventListener('resize', updateSize);
        cancelAnimationFrame(requestRef.current);
    };
  }, [settings]);

  return (
    <canvas ref={canvasRef} className="w-full h-full object-cover" />
  );
}
