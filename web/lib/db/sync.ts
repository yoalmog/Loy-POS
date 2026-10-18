import { supabase } from '../supabase/client'
import { idbGetAll, idbPut, idbDelete } from './idb'
import { Receipt, StockMovement } from '../types'

export interface SyncStatus {
  isOnline: boolean
  isSyncing: boolean
  pendingCount: number
  lastSyncTime: string | null
  lastError: string | null
}

export interface OfflineQueueItem {
  id: string
  type: 'RECEIPT' | 'STOCK_MOVEMENT'
  payload: any
  timestamp: string
  retryCount: number
}

class SyncEngine {
  private listeners: ((status: SyncStatus) => void)[] = []
  private status: SyncStatus = {
    isOnline: typeof navigator !== 'undefined' ? navigator.onLine : true,
    isSyncing: false,
    pendingCount: 0,
    lastSyncTime: null,
    lastError: null,
  }

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('online', () => {
        this.status.isOnline = true
        this.notify()
        this.syncPendingQueue()
      })

      window.addEventListener('offline', () => {
        this.status.isOnline = false
        this.notify()
      })

      // Update pending count periodically
      setInterval(() => {
        this.refreshPendingCount()
      }, 10000)

      this.refreshPendingCount()
    }
  }

  public subscribe(listener: (status: SyncStatus) => void) {
    this.listeners.push(listener)
    listener(this.status)
    return () => {
      this.listeners = this.listeners.filter((l) => l !== listener)
    }
  }

  private notify() {
    this.listeners.forEach((l) => l({ ...this.status }))
  }

  public async refreshPendingCount(): Promise<number> {
    try {
      const queue = await idbGetAll<OfflineQueueItem>('offline_queue')
      this.status.pendingCount = queue.length
      this.notify()
      return queue.length
    } catch {
      return 0
    }
  }

  public async enqueue(type: 'RECEIPT' | 'STOCK_MOVEMENT', payload: any): Promise<void> {
    const queueItem: OfflineQueueItem = {
      id: `${type}_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
      type,
      payload,
      timestamp: new Date().toISOString(),
      retryCount: 0,
    }
    await idbPut('offline_queue', queueItem)
    await this.refreshPendingCount()

    // Try to sync immediately if online
    if (this.status.isOnline) {
      this.syncPendingQueue()
    }
  }

  public async syncPendingQueue(): Promise<void> {
    if (this.status.isSyncing) return
    this.status.isSyncing = true
    this.notify()

    try {
      const queue = await idbGetAll<OfflineQueueItem>('offline_queue')
      if (queue.length === 0) {
        this.status.isSyncing = false
        this.status.lastSyncTime = new Date().toLocaleTimeString()
        this.notify()
        return
      }

      for (const item of queue) {
        try {
          if (item.type === 'RECEIPT') {
            const receipt = item.payload as Receipt
            // Insert into Supabase receipts
            const { error } = await supabase.from('receipts').upsert({
              id: receipt.id,
              receipt_number: receipt.receipt_number,
              created_at: receipt.created_at,
              shift_id: receipt.shift_id,
              cashier_name: receipt.cashier_name,
              ticket_name: receipt.ticket_name,
              dining_option: receipt.dining_option,
              subtotal: receipt.subtotal,
              tax_amount: receipt.tax_amount,
              discount_amount: receipt.discount_amount,
              tip_amount: receipt.tip_amount,
              total_amount: receipt.total_amount,
              payment_type: receipt.payment_type,
              payment_details_json: receipt.payment_details_json,
              customer_id: receipt.customer_id,
              customer_name: receipt.customer_name,
              is_refunded: receipt.is_refunded,
              refund_timestamp: receipt.refund_timestamp,
              items_summary_json: receipt.items_summary_json,
            })

            if (!error) {
              await idbDelete('offline_queue', item.id)
            } else {
              console.warn('Sync receipt error, will retry later:', error.message)
            }
          } else if (item.type === 'STOCK_MOVEMENT') {
            const movement = item.payload as StockMovement
            // Update stock in Supabase items if available
            await supabase.rpc('adjust_item_stock', {
              p_item_id: movement.item_id,
              p_delta: movement.quantity_changed,
            })
            await idbDelete('offline_queue', item.id)
          }
        } catch (itemErr: any) {
          console.warn(`Error processing queue item ${item.id}:`, itemErr)
          item.retryCount += 1
          await idbPut('offline_queue', item)
        }
      }

      this.status.lastSyncTime = new Date().toLocaleTimeString()
      this.status.lastError = null
    } catch (err: any) {
      this.status.lastError = err.message || 'Sync failed'
      console.error('Sync failed:', err)
    } finally {
      this.status.isSyncing = false
      await this.refreshPendingCount()
      this.notify()
    }
  }
}

export const syncEngine = new SyncEngine()
