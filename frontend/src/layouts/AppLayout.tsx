import { NavLink, Outlet } from 'react-router-dom'
import styles from './AppLayout.module.css'

const navItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/expenses', label: 'Expenses', end: false },
  { to: '/analytics', label: 'Analytics', end: false },
] as const

function NavItems({ className }: { className: (props: { isActive: boolean }) => string }) {
  return (
    <>
      {navItems.map((item) => (
        <NavLink key={item.to} to={item.to} end={item.end} className={className}>
          {item.label}
        </NavLink>
      ))}
      <span className={`${styles.navLink} ${styles.navLinkDisabled}`} title="Coming in Stage 9">
        Assistant
      </span>
    </>
  )
}

export function AppLayout() {
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`

  return (
    <div className={styles.app}>
      <nav className={styles.side} aria-label="Main">
        <div className={styles.brand}>
          <span className={styles.logo} aria-hidden>
            ₹
          </span>
          Expenses
        </div>
        <NavItems className={linkClass} />
        <div className={styles.me}>Dev user (Stage 4)</div>
      </nav>

      <header className={styles.topBar}>
        <div className={styles.brand}>
          <span className={styles.logo}>₹</span>
          Expenses
        </div>
      </header>

      <main className={styles.main}>
        <Outlet />
      </main>

      <nav className={styles.bottomNav} aria-label="Mobile">
        {navItems.map((item) => (
          <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => (isActive ? 'active' : '')}>
            {item.label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}