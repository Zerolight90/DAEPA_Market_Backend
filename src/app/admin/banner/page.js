import { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  ArrowDown,
  ArrowUp,
  Eye,
  EyeOff,
  Image as ImageIcon,
  Loader2,
  Plus,
  RefreshCcw,
  Trash2
} from "lucide-react";
import styles from "../admin.module.css";

const API_BASE = process.env.NEXT_PUBLIC_BACKEND_URL ?? "http://localhost:8080";
