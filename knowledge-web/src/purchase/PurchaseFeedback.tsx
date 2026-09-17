import { Alert, Button, Skeleton } from 'antd';
import { usePurchase } from './PurchaseContext';

export function PurchaseFeedback() {
  const { status, error, notice, busy, store } = usePurchase();
  return <div className="purchase-feedback">
    {error ? <Alert type="error" showIcon message={error} action={<Button disabled={busy} onClick={() => void store.load()}>刷新结果</Button>} /> : null}
    {notice ? <Alert type="info" showIcon message={notice} /> : null}
    {status === 'loading' ? <div role="status"><Skeleton active />正在加载</div> : null}
  </div>;
}
