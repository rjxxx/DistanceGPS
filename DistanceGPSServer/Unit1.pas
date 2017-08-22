unit Unit1;

interface

uses
  Winapi.Windows, Winapi.Messages, System.SysUtils, System.Variants, System.Classes, Vcl.Graphics,
  Vcl.Controls, Vcl.Forms, Vcl.Dialogs, IdContext, IdBaseComponent, IdComponent,
  IdCustomTCPServer, IdTCPServer, Vcl.StdCtrls, 	SyncObjs, Vcl.Buttons, IdGlobal,
  IdTCPConnection, System.StrUtils, Vcl.Menus, Winapi.ShellAPI;

type

  TDevice = class
  public
    name:string[30];
    IP:string[15];
    isEnableGPS:string[1];
    //gpsStatus:string[2];
    Latitude, Longitude, Altitude, Accuracy: string[20];
  constructor Create(IP:string);
  end;

  TForm1 = class(TForm)
    IdTCPServer1: TIdTCPServer;
    SpeedButton1: TSpeedButton;
    Edit2: TEdit;
    Button1: TButton;
    Label1: TLabel;
    SpeedButton2: TSpeedButton;
    SpeedButton3: TSpeedButton;
    SpeedButton4: TSpeedButton;
    SpeedButton5: TSpeedButton;
    SpeedButton6: TSpeedButton;
    Memo1: TMemo;
    PopupMenu1: TPopupMenu;
    N1: TMenuItem;
    procedure IdTCPServer1Connect(AContext: TIdContext);
    procedure IdTCPServer1Execute(AContext: TIdContext);
    procedure IdTCPServer1Disconnect(AContext: TIdContext);
    procedure FormCreate(Sender: TObject);
    procedure Button1Click(Sender: TObject);
    procedure SpeedButton1MouseEnter(Sender: TObject);
    procedure SpeedButton1MouseDown(Sender: TObject; Button: TMouseButton;
      Shift: TShiftState; X, Y: Integer);
    procedure SpeedButton1MouseUp(Sender: TObject; Button: TMouseButton;
      Shift: TShiftState; X, Y: Integer);
    procedure FormClose(Sender: TObject; var Action: TCloseAction);
    procedure Log(msg:string; ip:string);
    procedure PopupMenu1Popup(Sender: TObject);
    procedure N1Click(Sender: TObject);
    procedure FormDestroy(Sender: TObject);
    procedure SendDataDevice(dev:TDevice) ;
    procedure OnOffGPS(ip:string; switch:Boolean);
    procedure sendSwitchOffGPS;
    procedure DisconnectDevice(dev:TDevice);
  private
    { Private declarations }
  public
    { Public declarations }
  end;



  TViewer = class
  public
    IP:string[15];
  constructor Create(IP:string);
  end;

var
  Form1: TForm1;
  section:TCriticalSection;
  Btn:array of TButton;
  Devices, Viewer:TList;
  currentButtonSendler:TObject;
  haveCurrentViewer:Boolean = false;

implementation

{$R *.dfm}

procedure TForm1.Log(msg:string; ip:string);
var
  t:TTime;
begin
  t:=Now;
  Form1.Memo1.Lines.Add(TimeToStr(t)+': ('+ ip + ') ' +msg );
end;

procedure TForm1.N1Click(Sender: TObject);
var
  i:Integer;
  s:string;
begin
  for I := 0 to Devices.Count-1 do
  if TDevice(Devices[i]).IP = TSpeedButton(currentButtonSendler).Hint then
  begin
    s:='https://www.google.ru/maps/place/' + TDevice(Devices[i]).Latitude+','+TDevice(Devices[i]).Longitude;
    ShellExecute(0, 'open', PWideChar(s), nil, nil, SW_SHOW);
    Break;
  end;
end;

procedure TForm1.OnOffGPS(ip: string; switch:Boolean);
var
  List:TList;
  i:Integer;
begin
  List:=IdTCPServer1.Contexts.LockList;
  try
    for i:=0 to List.Count-1 do
    begin
      if TIdContext(List.Items[i]).Connection.Socket.Binding.PeerIP = ip then
      begin
        if switch then
          TIdContext(List.Items[i]).Connection.Socket.WriteLn('10')
        else
          TIdContext(List.Items[i]).Connection.Socket.WriteLn('11');

        Break;
      end;
    end;
  finally
    IdTCPServer1.Contexts.UnlockList;//ОБЯЗАТЕЛЬНО!!!
  end;
end;

procedure TForm1.PopupMenu1Popup(Sender: TObject);
begin
  currentButtonSendler := (PopupMenu1.PopupComponent as TSpeedButton);
end;

function ParseName(s:string):string;
begin
  if Copy(s,1,4) = 'NAME' then
  begin
    Result:=Copy(s,5,length(s));
    Result:=Trim(Result);
  end  else
    Result:='';
end;



procedure TForm1.Button1Click(Sender: TObject);
var
  List:TList;
  i:Integer;
begin
  if Button1.Caption = 'Запустить сервер' then
  begin
    try
      IdTCPServer1.Bindings.Clear;
      IdTCPServer1.Bindings.Add.IP:='0.0.0.0';
      IdTCPServer1.Bindings.Add.Port:=StrToInt(Edit2.Text);
    except
       ShowMessage('Не правильно введен порт');
    end;
    IdTCPServer1.Active:=True;

    Button1.Caption:='Остановить сервер';
    Edit2.Enabled:=False;
    Form1.Caption:='Сервер работает';
  end else
  begin
    Form1.Caption:='Сервер отключен';

   List:=IdTCPServer1.Contexts.LockList;
   try
     for i:=0 to List.Count-1 do
      TIdContext(List.Items[i]).Connection.Disconnect;
   finally
     IdTCPServer1.Contexts.UnlockList;
   end;


    IdTCPServer1.Scheduler.ActiveYarns.Clear;
    IdTCPServer1.Active:=False;

    Button1.Caption := 'Запустить сервер';
    Edit2.Enabled:=True;
  end;

end;

procedure TForm1.FormClose(Sender: TObject; var Action: TCloseAction);
var
  List:TList;
  i:Integer;
begin

  List:=IdTCPServer1.Contexts.LockList;
  try
    for i:=0 to List.Count-1 do
      TIdContext(List.Items[i]).Connection.Disconnect;

    IdTCPServer1.Contexts.UnlockList;//ОБЯЗАТЕЛЬНО!!!

    IdTCPServer1.Scheduler.ActiveYarns.Clear;
    IdTCPServer1.Active:=False;
  except

  end;

end;

procedure TForm1.FormCreate(Sender: TObject);
begin
  section := TCriticalSection.Create;
  Devices := TList.Create;
  Viewer := TList.Create;
end;


procedure TForm1.FormDestroy(Sender: TObject);
begin
  Devices.free;
  Viewer.Free;
end;

procedure TForm1.IdTCPServer1Connect(AContext: TIdContext);
//var
 // i:integer;
 // SB:TSpeedButton;
begin
  Log('Connect',AContext.Connection.Socket.Binding.PeerIP);

  Devices.Add(TDevice.Create(AContext.Connection.Socket.Binding.PeerIP));


  {for i := 0 to Length(Btn)-1 do
  begin
    if Btn[i] = nil then
    begin
      Btn[i]:=TButton.Create(Form1);
      Btn[i].Parent:=Form1;
      Btn[i].Left:=0;
      Btn[i].Top:=0;
      Btn[i].Width:=100;
      Btn[i].Height:=50;
      Btn[i].OnClick:=BtnClick;
      Exit;
    end;
  end;

  i:=Length(Btn)+1;
  SetLength(Btn,i);
  Dec(i);
  Btn[i]:=TButton.Create(Form1);
  Btn[i].Parent:=Form1;
  Btn[i].Left:=0;
  Btn[i].Top:=0;
  Btn[i].Width:=100;
  Btn[i].Height:=50;
  Btn[i].OnClick:=BtnClick;
    }
end;

procedure TForm1.DisconnectDevice(dev:TDevice);
var
  List:TList;
  i, j:Integer;
begin
  if Viewer.Count = 0 then  Exit;

  List:=IdTCPServer1.Contexts.LockList;
  try
    for j := 0 to Viewer.Count - 1 do
      for i:=0 to List.Count-1 do
        if TIdContext(List.Items[i]).Connection.Socket.Binding.PeerIP = TViewer(Viewer.Items[j]).IP then
        begin
          TIdContext(List.Items[i]).Connection.Socket.WriteLn('Disconnect:'+dev.IP);
          Break;
        end;
  finally
    IdTCPServer1.Contexts.UnlockList;//ОБЯЗАТЕЛЬНО!!!
  end;
end;

procedure TForm1.IdTCPServer1Disconnect(AContext: TIdContext);
var
  i:integer;
  SB:TSpeedButton;
begin
  Log('Disconect',AContext.Connection.Socket.Binding.PeerIP);

  for I := 1 to 6 do
  begin
    SB:=FindComponent('SpeedButton'+IntToStr(i)) As TSpeedButton;
    if SB.Hint = AContext.Connection.Socket.Binding.PeerIP then
    begin
      SB.Enabled:=False;
      SB.Hint:='';
      SB.Caption:='Пусто';
      Break;
    end;

  end;

  for I := 0 to Devices.Count-1 do
  if TDevice(Devices[i]).IP = AContext.Connection.Socket.Binding.PeerIP then
  begin
    DisconnectDevice(Devices[i]);
    Devices.Delete(i);
    Exit;
  end;

  for I := 0 to Viewer.Count-1 do
  if TViewer(Viewer[i]).IP = AContext.Connection.Socket.Binding.PeerIP then
  begin
    Viewer.Delete(i);
    sendSwitchOffGPS;
    Break;
  end;


end;

procedure TForm1.SendDataDevice(dev:TDevice) ;
var
  List:TList;
  i, j:Integer;
begin
  if Viewer.Count = 0 then  Exit;

  List:=IdTCPServer1.Contexts.LockList;
  try
    for j := 0 to Viewer.Count - 1 do
      for i:=0 to List.Count-1 do
        if TIdContext(List.Items[i]).Connection.Socket.Binding.PeerIP = TViewer(Viewer.Items[j]).IP then
        begin
          TIdContext(List.Items[i]).Connection.Socket.WriteLn('Information:'+dev.name + #9 +
                                                              dev.IP + #9 +
                                                              dev.isEnableGPS + #9 +
                                                              //dev.gpsStatus + #9 +
                                                              dev.Latitude + #9 +
                                                              dev.Longitude + #9 +
                                                              dev.Altitude + #9 +
                                                              dev.Accuracy, IndyTextEncoding_UTF8);
          Break;
        end;
  finally
    IdTCPServer1.Contexts.UnlockList;//ОБЯЗАТЕЛЬНО!!!
  end;
end;

procedure TForm1.sendSwitchOffGPS;
var
  List:TList;
  i, j:Integer;
begin
  List:=IdTCPServer1.Contexts.LockList;
  try
    for j := 0 to Devices.Count - 1 do
      for i:=0 to List.Count-1 do
        if TIdContext(List.Items[i]).Connection.Socket.Binding.PeerIP = TDevice(Devices.Items[j]).IP then
        begin
          TIdContext(List.Items[i]).Connection.Socket.WriteLn('11');
        end;
  finally
    IdTCPServer1.Contexts.UnlockList;//ОБЯЗАТЕЛЬНО!!!
  end;
end;


procedure TForm1.IdTCPServer1Execute(AContext: TIdContext);
var
  strText: String;
  i,j, posCoord:integer;
  SB:TSpeedButton;
begin
  try
    //Принимаем от клиента строку
    //strText := AContext.Connection.Socket.ReadLn;
    strText := AContext.Connection.IOHandler.ReadLn(IndyTextEncoding_UTF8);
    //Отвечаем
  //  AContext.Connection.Socket.WriteLn('Took the line: ' + strText);
    //Обновим сведения на форме сервера (сервер многопоточный,
    //поэтому используем синхронизацию
    if strText = '9' then
    begin
      AContext.Connection.Socket.WriteLn('9');
      Exit;
    end;

    section.Enter;

    Log(strText, AContext.Connection.Socket.Binding.PeerIP);

    for I := 0 to Devices.Count-1 do
      if TDevice(Devices[i]).IP = AContext.Connection.Socket.Binding.PeerIP then
      begin
        if Copy(strText,1,4) = 'NAME' then
        begin
          for j := 1 to 6 do
          begin
            SB:=FindComponent('SpeedButton'+IntToStr(j)) As TSpeedButton;
            if (SB.Enabled = False) or (SB.Caption = ParseName(strText)) then
            begin
              SB.Enabled:=True;
              SB.Hint:=AContext.Connection.Socket.Binding.PeerIP;
              SB.Caption:=ParseName(strText);

              Break;
            end;
          end;
          TDevice(Devices[i]).name:=ParseName(strText);
          //SendDataDevice(Devices[i]);
          Break;
        end;

        if Copy(strText,1,11) = 'isEnableGPS' then
        begin
          TDevice(Devices[i]).isEnableGPS:=Copy(strText,13,1);
        { if Copy(strText,13,1) = '0' then
          begin
             TDevice(Devices[i]).gpsStatus:='-1';
          end;
         }
          SendDataDevice(Devices[i]);
          Break;
        end;

        {
       if Copy(strText,1,9) = 'gpsStatus' then
        begin
          TDevice(Devices[i]).gpsStatus:=Copy(strText,11,1);
          SendDataDevice(Devices[i]);
          Break;
        end;
       }

        if Copy(strText,1,11) = 'Coordinates' then
        begin
          posCoord:=Pos('Lat:', strText) + 4;
          TDevice(Devices[i]).Latitude :=StringReplace(Copy(strText, posCoord, PosEx(';', strText, posCoord) - posCoord), ',', '.', []);

          posCoord:=Pos('Lon:', strText) + 4;
          TDevice(Devices[i]).Longitude := StringReplace(Copy(strText, posCoord, PosEx(';', strText, posCoord) - posCoord), ',', '.',[]);

          posCoord:=Pos('Alt:', strText) + 4;
          TDevice(Devices[i]).Altitude := StringReplace(Copy(strText, posCoord, PosEx(';', strText, posCoord) - posCoord), ',', '.',[]);

          posCoord:=Pos('Acc:', strText) + 4;
          TDevice(Devices[i]).Accuracy := Copy(strText, posCoord, PosEx(',', strText, posCoord) - posCoord + 2);

          SendDataDevice(Devices[i]);
          Break;
        end;


        if strText = 'isViewer' then
        begin
          Devices.Delete(i);
          Viewer.Add(TViewer.Create(AContext.Connection.Socket.Binding.PeerIP));
          for j := 0 to Devices.Count-1 do
            SendDataDevice(Devices[j]);
          Break;
        end;



        {
        Log('====================');
        Log(TDevice(Devices[i]).name);
        Log(TDevice(Devices[i]).IP);
        Log(TDevice(Devices[i]).isEnableGPS);
        Log(TDevice(Devices[i]).Latitude);
        Log(TDevice(Devices[i]).Longitude);
        Log(TDevice(Devices[i]).Altitude);
        Log(TDevice(Devices[i]).Accuracy);
        Log('====================');
        }
      end;

      for I := 0 to Viewer.Count-1 do
        if TViewer(Viewer[i]).IP = AContext.Connection.Socket.Binding.PeerIP then
        begin

          if Copy(strText,1,13) = 'TrackingStart' then
          begin
            OnOffGPS(Copy(strText,15, length(strText) - 14), true);
            Break;
          end;

          if Copy(strText,1,12) = 'TrackingStop' then
          begin
            OnOffGPS(Copy(strText,14, length(strText) - 13), false);
            Break;
          end;

        end;


    //Memo1.Lines.add(AContext.Connection.Socket.Binding.PeerIP + ' сказал: ' + strText);

    section.Leave;

  except

  end;
end;

procedure TForm1.SpeedButton1MouseDown(Sender: TObject; Button: TMouseButton;
  Shift: TShiftState; X, Y: Integer);
var
  List:TList;
  i:Integer;
begin
  if Button = mbLeft then
  begin
    List:=IdTCPServer1.Contexts.LockList;
    try
      for i:=0 to List.Count-1 do
      if TIdContext(List.Items[i]).Connection.Socket.Binding.PeerIP = (Sender as TSpeedButton).Hint then
      begin
        TIdContext(List.Items[i]).Connection.Socket.WriteLn('0');
        Break;
      end;
    finally
      IdTCPServer1.Contexts.UnlockList;//ОБЯЗАТЕЛЬНО!!!
    end;
  end;
end;


procedure TForm1.SpeedButton1MouseEnter(Sender: TObject);
begin
  application.HintPause := 1000;
  (Sender as TSpeedButton).ShowHint := true;
end;

procedure TForm1.SpeedButton1MouseUp(Sender: TObject; Button: TMouseButton;
  Shift: TShiftState; X, Y: Integer);
var
  List:TList;
  i:Integer;
begin
  if Button = mbLeft then
  begin
    List:=IdTCPServer1.Contexts.LockList;
    try
      for i:=0 to List.Count-1 do
      if TIdContext(List.Items[i]).Connection.Socket.Binding.PeerIP = (Sender as TSpeedButton).Hint then
      begin
        TIdContext(List.Items[i]).Connection.Socket.WriteLn('1');
        Break;
      end;
    finally
      IdTCPServer1.Contexts.UnlockList;//ОБЯЗАТЕЛЬНО!!!
    end;
  end;
end;

{ TDevice }

constructor TDevice.Create(IP: string);
begin
  Self.IP:=IP;
  Self.name:='';
  Self.isEnableGPS:='0';
  //Self.gpsStatus:='-1';
  Self.Latitude:='';
  Self.Longitude:='';
  Self.Altitude:='';
  Self.Accuracy:='';
end;

{ TViewer }

constructor TViewer.Create(IP: string);
begin
  Self.IP := IP;
end;

end.
