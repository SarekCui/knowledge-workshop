import { Pagination } from 'antd';
import { usePurchase } from './PurchaseContext';
export function PurchasePagination() {
  const { page, total, busy, store } = usePurchase();
  return total > 20 ? <Pagination className="course-pagination" current={page} total={total} pageSize={20} showSizeChanger={false}
    disabled={busy} onChange={value => void store.load(value)} /> : null;
}
