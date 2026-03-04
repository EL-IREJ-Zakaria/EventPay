/**
 * EventPay Web - TypeScript Types
 * 
 * These types mirror the data models from the Android app
 * for consistency across platforms.
 */

export type UserRole = 'ADMIN' | 'SCANNER' | 'ATTENDEE';

export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'ONGOING' | 'COMPLETED' | 'CANCELLED';

export type TicketStatus = 'ACTIVE' | 'USED' | 'REFUNDED' | 'CANCELLED' | 'EXPIRED';

export type TicketType = 'STANDARD' | 'VIP' | 'PREMIUM' | 'EARLY_BIRD' | 'STUDENT' | 'GROUP';

export type CheckInResult = 'SUCCESS' | 'ALREADY_SCANNED' | 'INVALID' | 'NOT_FOUND' | 'EXPIRED' | 'WRONG_EVENT' | 'CANCELLED' | 'ERROR';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  isActive: boolean;
  createdAt: number;
  lastLoginAt?: number;
  phone?: string;
  profileImageUrl?: string;
  organization?: string;
  assignedEvents?: string[];
  walletBalance: number;
  preferences: {
    notificationsEnabled: boolean;
    emailNotifications: boolean;
    darkMode: boolean;
    language: string;
  };
}

export interface Event {
  id: string;
  name: string;
  description: string;
  location: string;
  date: number;
  endDate: number;
  ticketPrice: number;
  totalTickets: number;
  soldTickets: number;
  organizerId: string;
  organizerName?: string;
  createdAt: number;
  imageUrl?: string;
  category: string;
  status: EventStatus;
  startTime: string;
  endTime: string;
  capacity: number;
  checkedInCount: number;
  vipPrice?: number;
  vipTickets: number;
  vipSold: number;
  isPublished: boolean;
  tags: string[];
  contactEmail?: string;
  contactPhone?: string;
  website?: string;
  assignedScanners: string[];
}

export interface Ticket {
  id: string;
  eventId: string;
  userId: string;
  ticketType: TicketType;
  price: number;
  purchaseDate: number;
  status: TicketStatus;
  isCheckedIn: boolean;
  checkedInAt?: number;
  checkedInBy?: string;
  qrCode: string;
  attendeeName?: string;
}

export interface CheckInRecord {
  id: string;
  ticketId: string;
  eventId: string;
  userId: string;
  scannedBy: string;
  scannedByName: string;
  scannedByRole: UserRole;
  scannedAt: number;
  deviceId: string;
  result: CheckInResult;
  message?: string;
  previousScanId?: string;
  previousScanTime?: number;
}

export interface DashboardStats {
  totalEvents: number;
  totalTicketsSold: number;
  totalRevenue: number;
  totalCheckIns: number;
  activeScanners: number;
  upcomingEvents: number;
}

export interface ScannerSessionStats {
  totalScans: number;
  successfulScans: number;
  failedScans: number;
  successRate: number;
}

export interface CreateEventRequest {
  name: string;
  description: string;
  location: string;
  date: number;
  endDate: number;
  ticketPrice: number;
  totalTickets: number;
  vipPrice?: number;
  vipTickets?: number;
  category: string;
  contactEmail?: string;
  contactPhone?: string;
}

export interface CreateScannerRequest {
  email: string;
  password: string;
  fullName: string;
  assignedEventIds: string[];
}
