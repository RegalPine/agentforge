// @agentforge/ui - Component library (GRP-UI Design System)

// Theme
export { ThemeProvider, useTheme } from "./theme/index.js";
export type { ThemeMode } from "./theme/index.js";
export { themeTokens } from "./theme/index.js";

// Layout
export { Box, boxVariants } from "./components/box/index.js";
export type { BoxProps } from "./components/box/index.js";
export { Stack, VStack, HStack, stackVariants } from "./components/stack/index.js";
export type { StackProps } from "./components/stack/index.js";
export { Flex, flexVariants } from "./components/flex/index.js";
export type { FlexProps } from "./components/flex/index.js";
export {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
} from "./components/card/index.js";

// Interaction
export { Button, buttonVariants } from "./components/button/index.js";
export type { ButtonProps } from "./components/button/index.js";
export { Input, inputVariants } from "./components/input/index.js";
export type { InputProps } from "./components/input/index.js";
export { Badge, badgeVariants } from "./components/badge/index.js";
export type { BadgeProps } from "./components/badge/index.js";
export {
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
} from "./components/tabs/index.js";
export { Separator } from "./components/separator/index.js";
export { ScrollArea, ScrollBar } from "./components/scroll-area/index.js";
export {
  Tooltip,
  TooltipTrigger,
  TooltipContent,
  TooltipProvider,
} from "./components/tooltip/index.js";

// Data Display
export {
  Table,
  TableHeader,
  TableBody,
  TableFooter,
  TableHead,
  TableRow,
  TableCell,
  TableCaption,
} from "./components/table/index.js";
export { Typography, typographyVariants } from "./components/typography/index.js";
export type { TypographyProps } from "./components/typography/index.js";

// Navigation
export { Link, linkVariants } from "./components/link/index.js";
export type { LinkProps } from "./components/link/index.js";
export {
  Sidebar,
  SidebarHeader,
  SidebarContent,
  SidebarFooter,
  SidebarSection,
  SidebarItem,
  SidebarCollapse,
  sidebarVariants,
} from "./components/sidebar/index.js";
export type { SidebarProps } from "./components/sidebar/index.js";

// Code
export { CodeBlock } from "./components/code-block/index.js";
export type { CodeBlockProps } from "./components/code-block/index.js";
export { InlineCode } from "./components/inline-code/index.js";
export type { InlineCodeProps } from "./components/inline-code/index.js";

// Form
export { Checkbox } from "./components/checkbox/index.js";
export type { CheckboxProps } from "./components/checkbox/index.js";
export { RadioGroup, RadioGroupItem } from "./components/radio/index.js";
export { Label } from "./components/label/index.js";
export { Textarea, textareaVariants } from "./components/textarea/index.js";
export type { TextareaProps } from "./components/textarea/index.js";
export {
  Select,
  SelectGroup,
  SelectValue,
  SelectTrigger,
  SelectContent,
  SelectLabel,
  SelectItem,
  SelectSeparator,
  SelectScrollUpButton,
  SelectScrollDownButton,
} from "./components/select/index.js";
export { Switch } from "./components/switch/index.js";
export type { SwitchProps } from "./components/switch/index.js";

// Feedback
export { Spinner, spinnerVariants } from "./components/spinner/index.js";
export type { SpinnerProps } from "./components/spinner/index.js";
export { Alert, AlertTitle, AlertDescription, alertVariants } from "./components/alert/index.js";
export type { AlertProps } from "./components/alert/index.js";
export { Progress } from "./components/progress/index.js";
export type { ProgressProps } from "./components/progress/index.js";
export { Skeleton } from "./components/skeleton/index.js";
export { Empty } from "./components/empty/index.js";
export type { EmptyProps } from "./components/empty/index.js";

// Overlay
export {
  Dialog,
  DialogPortal,
  DialogOverlay,
  DialogClose,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogFooter,
  DialogTitle,
  DialogDescription,
} from "./components/dialog/index.js";
export {
  Drawer,
  DrawerTrigger,
  DrawerClose,
  DrawerContent,
  DrawerHeader,
  DrawerFooter,
  DrawerTitle,
  DrawerDescription,
} from "./components/drawer/index.js";
export type { DrawerProps } from "./components/drawer/index.js";
export {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuCheckboxItem,
  DropdownMenuRadioItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuShortcut,
  DropdownMenuGroup,
  DropdownMenuPortal,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuRadioGroup,
} from "./components/dropdown-menu/index.js";

// Data Display (extended)
export { Avatar, AvatarImage, AvatarFallback, avatarVariants } from "./components/avatar/index.js";
export type { AvatarProps } from "./components/avatar/index.js";
export {
  Timeline,
  TimelineItem,
  TimelinePoint,
  TimelineConnector,
  TimelineContent,
  TimelineTime,
  TimelineTitle,
  TimelineDescription,
  timelineVariants,
} from "./components/timeline/index.js";
export type { TimelineProps } from "./components/timeline/index.js";
export {
  Descriptions,
  DescriptionsItem,
} from "./components/descriptions/index.js";
export type { DescriptionsProps, DescriptionsItemProps } from "./components/descriptions/index.js";

// Navigation (extended)
export {
  Breadcrumb,
  BreadcrumbList,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbPage,
  BreadcrumbSeparator,
  BreadcrumbEllipsis,
} from "./components/breadcrumb/index.js";
export { BreadcrumbIDE } from "./components/breadcrumb/index.js";
export type { BreadcrumbIDEProps, BreadcrumbSegment } from "./components/breadcrumb/index.js";

// IDE Layout Components
export { ActivityBar } from "./components/activity-bar/index.js";
export type { ActivityBarProps, ActivityBarItem } from "./components/activity-bar/index.js";
export { FileExplorer } from "./components/file-explorer/index.js";
export type { FileExplorerProps, FileExplorerGroup, FileExplorerItem } from "./components/file-explorer/index.js";
export { TabBar } from "./components/tab-bar/index.js";
export type { TabBarProps, TabBarItem } from "./components/tab-bar/index.js";
export { BottomPanel } from "./components/bottom-panel/index.js";
export type { BottomPanelProps, BottomPanelTab } from "./components/bottom-panel/index.js";
export { Terminal } from "./components/terminal/index.js";
export type { TerminalProps, TerminalLine } from "./components/terminal/index.js";
export { StatusBar } from "./components/status-bar/index.js";
export type { StatusBarProps, StatusBarItem } from "./components/status-bar/index.js";
export { TopBar } from "./components/top-bar/index.js";
export type { TopBarProps } from "./components/top-bar/index.js";

// IDE Data Display Components
export { MetricCard } from "./components/metric-card/index.js";
export type { MetricCardProps } from "./components/metric-card/index.js";
export { StatusLight } from "./components/status-light/index.js";
export type { StatusLightProps } from "./components/status-light/index.js";
export { ProgressBar } from "./components/progress-bar/index.js";
export type { ProgressBarProps } from "./components/progress-bar/index.js";
export { SparkLine } from "./components/sparkline/index.js";
export type { SparkLineProps } from "./components/sparkline/index.js";
export { CodeEditor } from "./components/code-editor/index.js";
export type { CodeEditorProps, CodeEditorLanguage } from "./components/code-editor/index.js";

// Visualization Components (Phase 3)
export { PipelineStage, PipelineChain } from "./components/pipeline-stage/index.js";
export type { PipelineStageProps, PipelineStageData, PipelineChainProps } from "./components/pipeline-stage/index.js";
export { LineChart } from "./components/line-chart/index.js";
export type { LineChartProps, LineChartDatum } from "./components/line-chart/index.js";
export { DonutChart } from "./components/donut-chart/index.js";
export type { DonutChartProps, DonutChartDatum } from "./components/donut-chart/index.js";
export { WaterfallChart } from "./components/waterfall-chart/index.js";
export type { WaterfallChartProps, WaterfallSpan } from "./components/waterfall-chart/index.js";
export { RadarChart } from "./components/radar-chart/index.js";
export type { RadarChartProps, RadarAxis, RadarDataset } from "./components/radar-chart/index.js";
