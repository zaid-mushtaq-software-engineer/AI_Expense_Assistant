import styles from './Skeleton.module.css'

export function SkeletonRows({ count = 5 }: { count?: number }) {
  return (
    <div aria-busy="true" aria-label="Loading">
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className={styles.row} />
      ))}
    </div>
  )
}