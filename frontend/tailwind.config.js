/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        bg:        '#080B16',
        surface:   '#101526',
        elevated:  '#151C30',
        primary:   '#8B5CF6',
        secondary: '#22D3EE',
        attention: '#F59E0B',
        critical:  '#F43F5E',
        healthy:   '#34D399',
        'text-main': '#F8FAFC',
        'text-muted': '#94A3B8',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Menlo', 'monospace'],
      },
    },
  },
  plugins: [],
}
