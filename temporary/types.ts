export enum AspectRatio {
  Portrait = "9:16",
  Landscape = "16:9",
  Square = "1:1"
}

export enum Environment {
  Forest = "Forest",
  Beach = "Beach",
  Snowy = "Snowy Field",
  SimpleBlack = "Pure Black"
}

export enum FuelType {
  Wood = "Wood",
  Paper = "Paper",
  Charcoal = "Charcoal",
  Chemical = "Chemical"
}

export enum Weather {
  Clear = "Clear",
  Rainy = "Rainy",
  Snowy = "Snowy",
  Windy = "Windy"
}

export interface BonfireSettings {
  aspectRatio: AspectRatio;
  environment: Environment;
  fuelType: FuelType;
  weather: Weather;
  intensity: number; // 0.1 to 2.0 (Amount of particles)
  temperature: number; // 1000 to 10000 (Kelvin approx, affects color/height)
  windSpeed: number; // -5 to 5
  playbackSpeed: number; // 0.1 to 3.0
  timerDuration: number; // Seconds. 0 means infinite.
  timerStartTime: number; // Unix timestamp
}