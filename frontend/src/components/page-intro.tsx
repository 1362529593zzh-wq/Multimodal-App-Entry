import type { ReactNode } from 'react';

interface IntroStat {
  label: string;
  value: ReactNode;
  hint?: string;
}

interface PageIntroProps {
  eyebrow: string;
  title: string;
  description: string;
  stats: IntroStat[];
  actions?: ReactNode;
}

export const PageIntro = ({ eyebrow, title, description, stats, actions }: PageIntroProps) => (
  <section className="page-intro">
    <div className="page-intro__copy">
      <span className="page-intro__eyebrow">{eyebrow}</span>
      <h1>{title}</h1>
      <p>{description}</p>
    </div>
    <div className="page-intro__aside">
      <div className="page-intro__stats">
        {stats.map((item) => (
          <div className="page-intro__stat" key={item.label}>
            <span>{item.label}</span>
            <strong>{item.value}</strong>
            {item.hint ? <small>{item.hint}</small> : null}
          </div>
        ))}
      </div>
      {actions ? <div className="page-intro__actions">{actions}</div> : null}
    </div>
  </section>
);
