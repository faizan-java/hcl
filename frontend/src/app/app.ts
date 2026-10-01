import { CommonModule } from '@angular/common';
import { Component, HostListener, ViewChild, ElementRef, isDevMode } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Appointment, CareDataService, CareRole, Invoice } from './core/care-data.service';
import { BodyAtlasComponent } from './features/body-map/body-atlas.component';

type Role = 'Patient' | 'Provider' | 'Admin';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, BodyAtlasComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  constructor(readonly careData: CareDataService) {}
  @ViewChild('globalSearch') globalSearch?: ElementRef<HTMLInputElement>;
  theme: 'light' | 'dark' = this.readTheme();
  get role(): Role { return this.careData.role(); }
  set role(value: Role) { this.careData.role.set(value); }
  get canSwitchRole() { return isDevMode(); }
  authEmail = '';
  authPassword = '';
  authName = '';
  registering = false;
  active = 'Overview';
  search = '';
  query = '';
  toast = '';
  menuOpen = false;
  showBooking = false;
  showNotifications = false;
  range = '7 days';
  bookingDate = new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10);
  bookingMinDate = new Date().toISOString().slice(0,10);
  bookingTime = '09:00';
  bookingSpecialty = 'Cardiology';
  bookingProvider = 'prv-0204';
  unread = 3;
  selectedBody = 'Heart';
  readonly departments = [
    { name: 'Cardiology', count: '18 providers', capacity: 82, color: 'teal' },
    { name: 'Primary care', count: '14 providers', capacity: 68, color: 'blue' },
    { name: 'Dermatology', count: '8 providers', capacity: 54, color: 'violet' },
  ];
  readonly activityBars = [
    { day: 'Mon', value: 48 }, { day: 'Tue', value: 72 }, { day: 'Wed', value: 58 },
    { day: 'Thu', value: 88 }, { day: 'Fri', value: 66 }, { day: 'Sat', value: 38 }, { day: 'Sun', value: 24 },
  ];
  nav: Record<Role, string[]> = {
    Patient: ['Overview', 'My profile', 'Appointments', '3D Body Map', 'Medical records', 'Prescriptions', 'Medications', 'Billing'],
    Provider: ['Overview', 'My patients', 'Appointments', 'Patient Body Map', 'Clinical records', 'Prescriptions', 'Schedule'],
    Admin: ['Overview', 'Users', 'Patients', 'Providers', 'Appointments', 'Analytics', 'Billing']
  };
  get navItems() { return this.nav[this.role]; }
  get appointments() { return this.careData.appointments(); }
  get summary() { return this.careData.overview(); }
  get pendingAppointments() { return this.summary?.appointments.pending ?? 0; }
  get confirmedAppointments() { return this.summary?.appointments.confirmed ?? 0; }
  get patientsTotal() { return this.summary?.patients ?? 0; }
  get providersTotal() { return this.summary?.providers ?? 0; }
  get completedVisits() { return this.appointments.filter(item=>item.status==='Completed').length; }
  get upcomingAppointments() { return this.appointments.filter(item => item.status === 'Confirmed' || item.status === 'Pending').filter(item => Date.parse(item.startsAt) >= Date.now()).sort((a,b)=>a.startsAt.localeCompare(b.startsAt)); }
  get nextAppointment(): Appointment | undefined { return this.upcomingAppointments[0]; }
  get activeMedications() { return this.careData.medications().filter(item => item.active); }
  get latestRecord() { return this.careData.records()[0]; }
  get latestVitalsDate() { const date=this.careData.vitals()[0]?.recordedAt;return date?`Updated ${new Date(date).toLocaleDateString('en-US',{month:'short',day:'numeric'})}`:'No readings yet'; }
  vitalRecorded(metric:string) { return this.careData.vitals().some(item=>item.metric===metric); }
  get selectedBodyVital() { const metric=this.selectedBody==='Heart'?'Heart rate':this.selectedBody==='Lungs'?'Oxygen saturation':'Body temperature';return this.careData.vitals().find(item=>item.metric===metric); }
  get bodyInsightTitle() { const reading=this.selectedBodyVital;return reading?(reading.status==='Normal'?`${this.selectedBody} looks in range`:`${this.selectedBody}: ${reading.status}`):`No ${this.selectedBody.toLowerCase()} reading yet`; }
  get bodyInsightDescription() { const reading=this.selectedBodyVital;return reading?`Latest ${reading.metric.toLowerCase()}: ${reading.value} ${reading.unit}.`:`Your care team has not added a recent ${this.selectedBody.toLowerCase()} measurement.`; }
  vitalValue(metric:string) { const vital=this.careData.vitals().find(item=>item.metric===metric);return vital?`${vital.value} ${vital.unit}`:'—'; }
  vitalNumber(metric:string) { return this.careData.vitals().find(item=>item.metric===metric)?.value ?? '—'; }
  vitalStatus(metric:string) { return this.careData.vitals().find(item=>item.metric===metric)?.status ?? 'Not recorded'; }
  get latestRecordDate() { const date=this.latestRecord?.recordedAt;return date?new Date(date).toLocaleDateString('en-US',{month:'short',day:'numeric',year:'numeric'}):'No recent update'; }
  get latestRecordTitle() { return this.latestRecord?.title ?? 'No health record on file'; }
  get openBalanceCents() { return this.careData.invoices().filter(item=>item.status==='Open'||item.status==='Overdue').reduce((total,item)=>total+item.amountCents,0); }
  get nextInvoice(): Invoice | undefined { return this.careData.invoices()[0]; }
  get formattedBalance() { return (this.openBalanceCents/100).toLocaleString('en-US',{style:'currency',currency:'USD'}); }
  get todayLabel() { return new Date().toLocaleDateString('en-US',{weekday:'long',month:'long',day:'numeric',year:'numeric'}).toUpperCase(); }
  get providerSchedule() { return this.careData.appointments().slice(0, 4); }
  get adminSchedule() { return this.careData.appointments().slice(0, 3); }
  get userName() { return this.role === 'Patient' ? 'Aarav Sharma' : this.role === 'Provider' ? 'Dr. Sarah Johnson' : 'Morgan Lee'; }
  get userInitials() { return this.role === 'Patient' ? 'AS' : this.role === 'Provider' ? 'SJ' : 'ML'; }
  get userCaption() { return this.role === 'Patient' ? 'Patient account' : this.role === 'Provider' ? 'Cardiology · Provider' : 'System administrator'; }
  get greeting() { const h = new Date().getHours(); return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening'; }
  get displayTitle() {
    if (this.active !== 'Overview') return this.active;
    return this.role === 'Patient' ? `${this.greeting}, Aarav` : this.role === 'Provider' ? `${this.greeting}, Sarah` : `${this.greeting}, Morgan`;
  }
  get pageDescription() {
    const descriptions: Record<string, string> = {
      'My profile': 'Manage your personal details and care preferences.', Appointments: 'Manage upcoming visits and stay connected with your care team.',
      '3D Body Map': 'Explore your body and securely report a concern to your care team.', 'Patient Body Map': 'Review assigned patients’ reported concerns and document clinical observations.',
      'Medical records': 'Your health history, visit summaries, and clinical notes in one place.', Prescriptions: 'View prescription details and refill requests from your providers.',
      Medications: 'Keep track of medications and your daily dose schedule.', Billing: 'Review invoices, payments, and account balances.',
      'My patients': 'A clear view of the people in your care.', 'Clinical records': 'Document visits and keep patient records up to date.',
      Schedule: 'Manage your clinic hours and upcoming visits.', Users: 'Manage CareConnect user access and roles.', Patients: 'Explore patient records and care status.',
      Providers: 'Manage provider profiles and clinical teams.', Analytics: 'Track clinic performance and care activity.', Settings: 'Manage your account preferences and workspace settings.'
    };
    return descriptions[this.active] || 'Your CareConnect workspace, designed around better care.';
  }
  get filteredAppointments() { return this.appointments.filter(a => `${a.name} ${a.specialty} ${a.status}`.toLowerCase().includes(this.query.toLowerCase())); }
  symptom='Pain'; bodyPart=''; bodyPartId=''; painLevel=5; symptomDuration='Today'; symptomDescription=''; complaintBusy=false;
  patientQuery=''; selectedComplaintId=''; diagnosis=''; doctorNotes=''; treatmentRecommendation=''; complaintStatus:'Open'|'Under Review'|'Resolved'='Open';
  get filteredCarePatients(){return this.careData.carePatients().filter(p=>`${p.name} ${p.id}`.toLowerCase().includes(this.patientQuery.toLowerCase()))}
  get selectedDoctorComplaint(){return this.careData.doctorBody()?.complaints.find(c=>c.id===this.selectedComplaintId)}

  selectPage(item: string) { this.active = item; this.menuOpen = false; this.query = ''; if(item==='3D Body Map') void this.careData.loadBodyComplaints(); if(item==='Patient Body Map') void this.careData.loadCarePatients(); }
  selectBodyPart(part:{bodyPart:string;bodyPartId:string}){this.bodyPart=part.bodyPart;this.bodyPartId=part.bodyPartId}
  async submitBodyComplaint(){if(!this.bodyPart||!this.symptom.trim()||!this.symptomDuration.trim()){this.notify('Select a body structure and complete the required fields.');return}this.complaintBusy=true;const ok=await this.careData.submitBodyComplaint({bodyPart:this.bodyPart,bodyPartId:this.bodyPartId,symptom:this.symptom.trim(),painLevel:this.painLevel,duration:this.symptomDuration.trim(),description:this.symptomDescription.trim()});this.complaintBusy=false;this.notify(ok?'Your concern was saved securely.':this.careData.error());if(ok){this.symptomDescription='';this.bodyPart='';this.bodyPartId=''}}
  async chooseCarePatient(id:string){await this.careData.loadDoctorBody(id);this.selectedComplaintId='';}
  selectComplaint(c:any){this.selectedComplaintId=c.id;this.diagnosis=c.diagnosis||'';this.doctorNotes=c.doctorNotes||'';this.treatmentRecommendation=c.treatmentRecommendation||'';this.complaintStatus=c.status}
  async saveDoctorReview(){const c=this.selectedDoctorComplaint;if(!c)return;const ok=await this.careData.updateDoctorComplaint(c.id,{diagnosis:this.diagnosis,doctorNotes:this.doctorNotes,treatmentRecommendation:this.treatmentRecommendation,status:this.complaintStatus});this.notify(ok?'Clinical review saved.':this.careData.error())}
  async removeBodyComplaint(id:string){const ok=await this.careData.deleteBodyComplaint(id);this.notify(ok?'Report removed.':this.careData.error())}
  primaryAction() {
    if (this.role === 'Patient') this.showBooking = true;
    else if (this.role === 'Provider') { this.selectPage('My patients'); this.notify('Patient directory opened'); }
    else { this.selectPage('Users'); this.notify('User management opened'); }
  }
  startVisit() { this.selectPage('Clinical records'); this.notify('Visit workspace ready'); }
  toggleTheme() {
    this.theme = this.theme === 'light' ? 'dark' : 'light';
    try { localStorage.setItem('careconnect-theme', this.theme); } catch { /* The theme still works when storage is unavailable. */ }
  }
  private readTheme(): 'light' | 'dark' {
    try { return localStorage.getItem('careconnect-theme') === 'dark' ? 'dark' : 'light'; } catch { return 'light'; }
  }
  @HostListener('window:keydown', ['$event'])
  handleShortcuts(event: KeyboardEvent) {
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.globalSearch?.nativeElement.focus();
    }
    if (event.key === 'Escape') {
      this.menuOpen = false;
      this.showNotifications = false;
      this.showBooking = false;
    }
  }
  async selectRole(role: Role) {
    this.role = role;
    this.active = 'Overview';
    this.menuOpen = false;
    this.showNotifications = false;
    await this.careData.setRole(role as CareRole);
    this.notify(this.careData.error() || `${role} portal selected`);
  }
  notify(message: string) { this.toast = message; window.setTimeout(() => this.toast = '', 3000); }
  searchNow() { this.query = this.search; if (this.search) this.active = 'Appointments'; }
  markRead() { this.unread = 0; this.showNotifications = false; this.notify('You’re all caught up'); }
  async bookAppointment() {
    const sent = await this.careData.requestAppointment({ specialty:this.bookingSpecialty, date:this.bookingDate, time:this.bookingTime, providerId:this.bookingProvider });
    if (sent) { this.showBooking = false; this.active = 'Appointments'; this.notify('Appointment request sent'); }
    else this.notify(this.careData.error());
  }
  async updateAppointment(appointment: Appointment, status: 'Confirmed' | 'Cancelled' | 'Completed') {
    const saved = await this.careData.updateAppointment(appointment.id, status);
    this.notify(saved ? `Appointment ${status.toLowerCase()}` : this.careData.error());
  }
  payBill() { this.notify('Payment provider is not connected yet.'); }
  async signIn() { const ok=await this.careData.login(this.authEmail,this.authPassword); if(ok)this.notify('Signed in successfully'); }
  async signUp() { const ok=await this.careData.register(this.authName,this.authEmail,this.authPassword); if(ok)this.notify('Account created successfully'); }
  async signInDemo(role: Role) { this.authEmail=`${role.toLowerCase()}@careconnect.local`;this.authPassword='CareConnectDemo2026!';await this.signIn(); }
  logout() { this.careData.logout();this.notify('Signed out'); }
}
