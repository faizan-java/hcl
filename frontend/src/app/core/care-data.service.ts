import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, isDevMode, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type CareRole = 'Patient' | 'Provider' | 'Admin';
export interface Appointment {
  id: string;
  name: string;
  specialty: string;
  date: string;
  time: string;
  startsAt: string;
  status: 'Confirmed' | 'Pending' | 'Cancelled' | 'Completed';
  initials: string;
  color: 'lavender' | 'mint' | 'peach';
  location?: string;
}
interface ApiEnvelope<T> { data: T; }
interface AuthResponse { token: string; user: { id: string; name: string; email: string; role: CareRole }; }
export interface Medication { id:string;name:string;dosage:string;instructions:string;active:number; }
export interface Invoice { id:string;invoiceNumber:string;description:string;amountCents:number;status:'Open'|'Paid'|'Overdue';dueAt:string;patientName?:string; }
export interface HealthRecord { id:string;title:string;summary:string;recordedAt:string;patientName?:string; }
export interface Vital { id:string;metric:string;value:number;unit:string;status:string;recordedAt:string; }
export interface CareOverview { appointments:{total:number;pending:number;confirmed:number};patients:number;providers:number;role:CareRole; }
export interface BodyComplaint { id:string;patientId:string;bodyPart:string;bodyPartId:string;symptom:string;painLevel:number;duration:string;description:string;diagnosis:string;doctorNotes:string;treatmentRecommendation:string;status:'Open'|'Under Review'|'Resolved';createdAt:string;updatedAt:string }
export interface CarePatient { id:string; name:string }

@Injectable({ providedIn: 'root' })
export class CareDataService {
  readonly appointments = signal<Appointment[]>([]);
  readonly medications = signal<Medication[]>([]);
  readonly invoices = signal<Invoice[]>([]);
  readonly records = signal<HealthRecord[]>([]);
  readonly vitals = signal<Vital[]>([]);
  readonly overview = signal<CareOverview | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly role = signal<CareRole>('Patient');
  readonly currentUserId = signal('');
  readonly bodyComplaints = signal<BodyComplaint[]>([]);
  readonly carePatients = signal<CarePatient[]>([]);
  readonly doctorBody = signal<{patient:CarePatient;complaints:BodyComplaint[]}|null>(null);
  private token = this.readToken();
  readonly authenticated = signal(Boolean(this.token));

  constructor(private readonly http: HttpClient) {
    this.loadAppointments();
  }

  async setRole(role: CareRole): Promise<void> {
    if (!isDevMode()) return;
    this.loading.set(true);
    this.error.set('');
    try {
      const response = await firstValueFrom(this.http.post<AuthResponse>(`/api/auth/demo-session/${role}`, {}));
      this.token = response.token;
      this.role.set(response.user.role);
      this.currentUserId.set(response.user.id);
      try { sessionStorage.setItem('careconnect-token', response.token); } catch { /* The current session remains usable without storage. */ }
      await this.loadAppointments();
    } catch (error) { this.setError(error); }
    finally { this.loading.set(false); }
  }

  async loadAppointments(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      if (!this.token) {
        if (!isDevMode()) return;
        const session = await firstValueFrom(this.http.post<AuthResponse>('/api/auth/demo-session/Patient', {}));
        this.token = session.token;
        this.role.set(session.user.role);
        this.currentUserId.set(session.user.id);
        try { sessionStorage.setItem('careconnect-token', session.token); } catch { /* Keep this tab functional without storage. */ }
      }
      const headers = this.headers();
      const [result, me, summary] = await Promise.all([
        firstValueFrom(this.http.get<ApiEnvelope<Appointment[]>>('/api/appointments', { headers })),
        firstValueFrom(this.http.get<{ user: { id: string; role: CareRole } }>('/api/me', { headers })),
        firstValueFrom(this.http.get<ApiEnvelope<CareOverview>>('/api/overview', { headers }))
      ]);
      this.authenticated.set(true);
      this.appointments.set(result.data);
      this.currentUserId.set(me.user.id);
      this.role.set(me.user.role);
      this.overview.set(summary.data);
      if (this.role() === 'Patient') {
        const [medications, invoices, records, vitals] = await Promise.all([
          firstValueFrom(this.http.get<ApiEnvelope<Medication[]>>('/api/medications', { headers })),
          firstValueFrom(this.http.get<ApiEnvelope<Invoice[]>>('/api/billing/invoices', { headers })),
          firstValueFrom(this.http.get<ApiEnvelope<HealthRecord[]>>('/api/records', { headers })),
          firstValueFrom(this.http.get<ApiEnvelope<Vital[]>>('/api/vitals', { headers }))
        ]);
        this.medications.set(medications.data);
        this.invoices.set(invoices.data);
        this.records.set(records.data);
        this.vitals.set(vitals.data);
      } else {
        this.medications.set([]);this.records.set([]);this.vitals.set([]);
        if(this.role()==='Admin'){
          const invoiceResult=await firstValueFrom(this.http.get<ApiEnvelope<Invoice[]>>('/api/billing/invoices',{headers:this.headers()}));
          this.invoices.set(invoiceResult.data);
        }else this.invoices.set([]);
      }
      if(this.role()==='Patient') await this.loadBodyComplaints();
      if(this.role()==='Provider') await this.loadCarePatients();
    } catch (error) { this.setError(error); }
    finally { this.loading.set(false); }
  }

  async login(email:string,password:string):Promise<boolean>{return this.authenticate('/api/auth/login',{email,password});}
  async register(name:string,email:string,password:string):Promise<boolean>{return this.authenticate('/api/auth/register',{name,email,password});}
  logout():void{this.token='';this.appointments.set([]);this.medications.set([]);this.invoices.set([]);this.records.set([]);this.vitals.set([]);this.overview.set(null);this.authenticated.set(false);this.role.set('Patient');try{sessionStorage.removeItem('careconnect-token')}catch{/* Current session is cleared. */}}
  private async authenticate(path:string,body:Record<string,string>):Promise<boolean>{
    this.loading.set(true);this.error.set('');
    try{
      const response=await firstValueFrom(this.http.post<AuthResponse>(path,body));
      this.token=response.token;this.role.set(response.user.role);this.authenticated.set(true);
      this.currentUserId.set(response.user.id);
      try{sessionStorage.setItem('careconnect-token',response.token)}catch{/* This tab remains authenticated in memory. */}
      await this.loadAppointments();return true;
    }catch(error){this.setError(error);return false}finally{this.loading.set(false)}
  }

  async requestAppointment(input: { specialty: string; date: string; time: string; providerId?: string }): Promise<boolean> {
    this.error.set('');
    const startsAt = new Date(`${input.date}T${input.time}:00`).toISOString();
    try {
      const result = await firstValueFrom(this.http.post<ApiEnvelope<Appointment>>('/api/appointments', {
        specialty: input.specialty, startsAt, ...(input.providerId ? { providerId: input.providerId } : {})
      }, { headers: this.headers() }));
      this.appointments.update(items => [...items, result.data].sort((a, b) => a.startsAt.localeCompare(b.startsAt)));
      return true;
    } catch (error) { this.setError(error); return false; }
  }

  async updateAppointment(id: string, status: 'Confirmed' | 'Cancelled' | 'Completed'): Promise<boolean> {
    try {
      await firstValueFrom(this.http.patch(`/api/appointments/${encodeURIComponent(id)}/status`, { status }, { headers: this.headers() }));
      this.appointments.update(items => items.map(item => item.id === id ? { ...item, status } : item));
      return true;
    } catch (error) { this.setError(error); return false; }
  }

  async loadBodyComplaints():Promise<void>{try{const id=this.currentUserId();if(!id)return;const r=await firstValueFrom(this.http.get<ApiEnvelope<BodyComplaint[]>>(`/api/patient/body-complaints/${encodeURIComponent(id)}`,{headers:this.headers()}));this.bodyComplaints.set(r.data)}catch(e){this.setError(e)}}
  async submitBodyComplaint(input:Pick<BodyComplaint,'bodyPart'|'bodyPartId'|'symptom'|'painLevel'|'duration'|'description'>):Promise<boolean>{try{const r=await firstValueFrom(this.http.post<ApiEnvelope<BodyComplaint>>('/api/patient/body-complaints',input,{headers:this.headers()}));this.bodyComplaints.update(items=>[r.data,...items]);return true}catch(e){this.setError(e);return false}}
  async deleteBodyComplaint(id:string):Promise<boolean>{try{await firstValueFrom(this.http.delete(`/api/patient/body-complaints/${encodeURIComponent(id)}`,{headers:this.headers()}));this.bodyComplaints.update(items=>items.filter(x=>x.id!==id));return true}catch(e){this.setError(e);return false}}
  async loadCarePatients():Promise<void>{try{const r=await firstValueFrom(this.http.get<ApiEnvelope<CarePatient[]>>('/api/doctor/patients',{headers:this.headers()}));this.carePatients.set(r.data)}catch(e){this.setError(e)}}
  async loadDoctorBody(patientId:string):Promise<void>{try{const r=await firstValueFrom(this.http.get<ApiEnvelope<{patient:CarePatient;complaints:BodyComplaint[]}>>(`/api/doctor/patients/${encodeURIComponent(patientId)}/body`,{headers:this.headers()}));this.doctorBody.set(r.data)}catch(e){this.setError(e)}}
  async updateDoctorComplaint(id:string,input:Partial<Pick<BodyComplaint,'diagnosis'|'doctorNotes'|'treatmentRecommendation'|'status'>>):Promise<boolean>{try{const r=await firstValueFrom(this.http.put<ApiEnvelope<BodyComplaint>>(`/api/doctor/body-complaints/${encodeURIComponent(id)}`,input,{headers:this.headers()}));this.doctorBody.update(body=>body?{...body,complaints:body.complaints.map(c=>c.id===id?r.data:c)}:body);return true}catch(e){this.setError(e);return false}}

  private headers(): HttpHeaders { return new HttpHeaders({ Authorization: `Bearer ${this.token}` }); }
  private readToken(): string { try { return sessionStorage.getItem('careconnect-token') ?? ''; } catch { return ''; } }
  private setError(error: unknown): void {
    if (error instanceof HttpErrorResponse && error.status === 401) this.logout();
    const message = error instanceof HttpErrorResponse
      ? (error.error && typeof error.error.error === 'string' ? error.error.error : 'Could not connect to CareConnect. Start the API with npm run dev and try again.')
      : 'Could not prepare the appointment time. Please choose another date.';
    this.error.set(message);
  }
}
