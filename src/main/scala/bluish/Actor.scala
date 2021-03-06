package bluish

sealed trait Actor {
  def pos: Vec
  def size: Vec

  def collide(state: State): State

  def update(dt: Double, state: State, keys: KeyState): Actor

  def overlaps(that: Actor) = {
    this.pos.x + this.size.x > that.pos.x &&
    this.pos.x < that.pos.x + that.size.x &&
    this.pos.y + this.size.y > that.pos.y &&
    this.pos.y < that.pos.y + that.size.y
  }
}

class Player(x: Double, y: Double, speed: Vec = Vec(0,0)) extends Actor {
  val pos = Vec(x,y)
  val size = Vec(0.8, 1.5)

  def collide(s: State) = s


  val playerXSpeed = 7.0
  val gravity = 30.0
  val jumpSpeed = 17.0

  def update(dt: Double, s: State, keys: KeyState) = {
    val xSpeed = (
      (if (keys.arrowLeft) -playerXSpeed else 0)
      + (if (keys.arrowRight) playerXSpeed else 0)
    )


    val movedX = pos + Vec(xSpeed * dt, 0)
    val updatedX = if (!s.level.touches(movedX, size, Wall)) movedX else pos

    var ySpeed = speed.y + dt*gravity
    val movedY = updatedX + Vec(0, ySpeed * dt)
    val updated =
      if (!s.level.touches(movedY, size, Wall))
        movedY
      else {
        if (keys.arrowUp && ySpeed > 0)
          ySpeed = -jumpSpeed
        else
          ySpeed = 0

        updatedX
      }
    new Player(updated.x, updated.y, speed = Vec(xSpeed, ySpeed))
  }
}

class Coin(x: Double, y: Double,
  wobblePhase: Double = math.random * math.Pi * 2) extends Actor {
  val basePos = Vec(x,y)

  val wobbleSpeed = 8
  val wobbleDist = 0.07

  val pos = Vec(x+0.2,y+0.1 + math.sin(wobblePhase)*wobbleDist)
  val size = Vec(0.6, 0.6)
  

  def collide(s: State) = {
    val others = s.actors.filter(a => a != this)
    val status =
      if (others.exists({
        case x: Coin => true
        case _ => false
      })) s.status
      else Won
    State(s.level, others, status)
  }

  def update(dt: Double, s: State, keys: KeyState) = {
    val wobble = wobblePhase + dt * wobbleSpeed
    new Coin(basePos.x , basePos.y, wobble)
  }
}

sealed trait Path
object BounceVertical extends Path
object BounceHorizontal extends Path
object Drip extends Path

class MovingLava(x: Double, y: Double, speed: Vec, reset: Option[Vec] = None) extends Actor {
  val pos = Vec(x,y)
  val size = Vec(1.0,1.0)

  def collide(s: State) = State(s.level, s.actors, Lost)

  def update(dt: Double, s: State, keys: KeyState) = {
    val newPos = pos + speed*dt
    if (!s.level.touches(newPos, size, Wall)) {
      new MovingLava(newPos.x, newPos.y, speed, reset)
    }
    else reset match {
      case Some(p) => new MovingLava(p.x, p.y, speed, Some(p))
      case None => new MovingLava(x, y, speed*(-1))
    }
  }
  // override def toString = s"[Lava @ (${pos.x},${pos.y})]"
}

// todo: actors take position vector

object Actor {
  def apply(char: Char)(i: Double, j: Double): Actor =
    char match {
      case '@' => new Player(i,j-0.5)
      case 'o' => new Coin(i,j)
      case '|' => new MovingLava(i,j,Vec(0,2))
      case '=' => new MovingLava(i,j,Vec(2,0))
      case 'v' => new MovingLava(i,j,Vec(0,3), Some(Vec(i,j)))
    }
}
